package gr1mly4memes.felt.mixin.core.util.datafix.fixes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.Typed;
import io.papermc.paper.event.server.AsyncServerDataFixerRemoveBlockEntityEvent;
import io.papermc.paper.math.Position;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.ChunkHeightAndBiomeFix;
import net.minecraft.util.datafix.fixes.RemoveBlockEntityTagFix;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataTypeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RemoveBlockEntityTagFix.class)
public abstract class RemoveBlockEntityTagFixMixin {
    @Unique private static final CraftPersistentDataTypeRegistry felt$dataTypes = new CraftPersistentDataTypeRegistry();

    @WrapOperation(method = "lambda$createChunkBlockEntityRemover$1", at = @At(value = "INVOKE",
        target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z", remap = false))
    private boolean felt$notifyBlockEntityRemoval(Set<?> removedTypes, Object id, Operation<Boolean> original,
                                                  @Local(argsOnly = true) Typed<?> chunk, @Local(index = 9) Typed<?> blockEntity) {
        boolean remove = original.call(removedTypes, id);
        if (!remove || !AsyncServerDataFixerRemoveBlockEntityEvent.getHandlerList().hasListeners()) {
            return remove;
        }
        var world = chunk.write().flatMap(data ->
            data.get(ChunkHeightAndBiomeFix.DATAFIXER_CONTEXT_TAG).get("level_identifier").asString()).result();
        var data = blockEntity.write().result();
        if (world.isPresent() && id instanceof String type && data.isPresent()) {
            var entity = data.orElseThrow();
            var x = entity.get("x").asNumber().result();
            var y = entity.get("y").asNumber().result();
            var z = entity.get("z").asNumber().result();
            if (x.isPresent() && y.isPresent() && z.isPresent()) {
                var container = new CraftPersistentDataContainer(felt$dataTypes);
                if (entity.get("PublicBukkitValues").orElseEmptyMap().convert(NbtOps.INSTANCE).getValue() instanceof CompoundTag nbt) {
                    container.putAll(nbt);
                }
                new AsyncServerDataFixerRemoveBlockEntityEvent(Key.key(world.orElseThrow()), Key.key(type),
                    Position.block(x.orElseThrow().intValue(), y.orElseThrow().intValue(), z.orElseThrow().intValue()), container).callEvent();
            }
        }
        return true;
    }
}
