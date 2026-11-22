package gr1mly4memes.felt.mixin.core.world.level.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.world.level.entity.PersistentEntitySectionManagerBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.Local;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<T extends EntityAccess> implements PersistentEntitySectionManagerBridge {

    // @formatter:off
    @Shadow public abstract void close() throws IOException;
    @Shadow @Final private EntityPersistentStorage<T> permanentStorage;
    @Shadow @Final EntitySectionStorage<T> sectionStorage;
    @Shadow @Final private Long2ObjectMap<PersistentEntitySectionManager.ChunkLoadStatus> chunkLoadStatuses;
    // @formatter:on

    @Override
    public void close(boolean save) throws IOException {
        if (save) {
            this.close();
        } else {
            this.permanentStorage.close();
        }
    }

    @Override
    public List<Entity> getEntities(ChunkPos chunkCoordIntPair) {
        return sectionStorage.getExistingSectionsInChunk(chunkCoordIntPair.pack())
            .flatMap(EntitySection::getEntities).map(o -> (Entity) o).collect(Collectors.toList());
    }

    @Override
    public List<Entity> bridge$getExistingEntities() {
        return this.sectionStorage.getAllChunksWithExistingSections().longStream()
            .mapToObj(this.sectionStorage::getExistingSectionsInChunk)
            .flatMap(sections -> sections)
            .flatMap(EntitySection::getEntities).map(entity -> (Entity) entity).collect(Collectors.toList());
    }

    @Override
    public boolean isPending(long cord) {
        return this.chunkLoadStatuses.get(cord) == PersistentEntitySectionManager.ChunkLoadStatus.PENDING;
    }

    @Unique private boolean felt$fireEvent = false;

    @Decorate(method = "storeChunkSections", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityPersistentStorage;storeEntities(Lnet/minecraft/world/level/entity/ChunkEntities;)V"))
    private void felt$fireUnload(long pos, @Local(ordinal = -1) List<T> list) {
        if (felt$fireEvent) {
            CraftEventFactory.callEntitiesUnloadEvent(((EntityStorage) permanentStorage).level, ChunkPos.unpack(pos),
                list.stream().map(entity -> (Entity) entity).collect(Collectors.toList()));
        }
    }

    @WrapMethod(method = "processChunkUnload")
    private boolean felt$unloadScope(long chunkPos, Operation<Boolean> original) {
        boolean previous = felt$fireEvent;
        felt$fireEvent = true;
        try {
            return original.call(chunkPos);
        } finally {
            felt$fireEvent = previous;
        }
    }

    @Inject(method = "processPendingLoads", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false, target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
    private void felt$fireLoad(CallbackInfo ci, ChunkEntities<T> chunkEntities) {
        List<Entity> entities = getEntities(chunkEntities.getPos());
        CraftEventFactory.callEntitiesLoadEvent(((EntityStorage) permanentStorage).level, chunkEntities.getPos(), entities);
    }

    @Inject(method = "unloadEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityAccess;setRemoved(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void felt$unloadCause(EntityAccess entityAccess, CallbackInfo ci) {
        if (entityAccess instanceof EntityBridge bridge) {
            bridge.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.UNLOAD);
        }
    }
}
