package gr1mly4memes.felt.mixin.core.world.entity;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import gr1mly4memes.felt.bridge.core.world.entity.EntityTypeBridge;
import gr1mly4memes.felt.bridge.core.world.level.IWorldWriterBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import net.minecraft.world.entity.PostSpawnProcessor;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> implements EntityTypeBridge<T> {

    // @formatter:off
    @Shadow @Nullable public abstract T create(ServerLevel p_262637_, @org.jetbrains.annotations.Nullable PostSpawnProcessor<T> p_262629_, BlockPos p_262595_, EntitySpawnReason p_262666_, boolean p_262685_, boolean p_262588_);
    // @formatter:on

    @Inject(method = "loadEntitiesRecursive", at = @At("HEAD"), cancellable = true)
    private static void felt$limitLoadedEntityTypes(ValueInput.ValueInputList entities, Level level, EntitySpawnReason reason,
                                                    CallbackInfoReturnable<Stream<Entity>> cir) {
        if (!(level instanceof ServerLevel world)) {
            return;
        }
        var config = ((WorldBridge) world).bridge$paperConfig();
        if (config == null) {
            return;
        }
        var limits = config.chunks.entityPerChunkSaveLimit;
        var counts = new Reference2IntOpenHashMap<EntityType<?>>();
        cir.setReturnValue(entities.stream().<Entity>mapMulti((tag, output) -> EntityType.loadEntityRecursive(tag, world, reason, entity -> {
            EntityType<?> type = entity.getType();
            int limit = limits.getOrDefault(type, -1);
            if (limit >= 0) {
                if (counts.getInt(type) >= limit) {
                    return null;
                }
                counts.addTo(type, 1);
            }
            output.accept(entity);
            return entity;
        })));
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;")
    private T felt$spawnReasonLifetime(ServerLevel worldIn, ItemStack p_20594_, LivingEntity p_20595_, BlockPos p_20596_, EntitySpawnReason p_20597_, boolean p_20598_, boolean p_20599_, com.llamalad7.mixinextras.injector.wrapoperation.Operation<T> original) throws Throwable {
        try {
            return original.call(worldIn, p_20594_, p_20595_, p_20596_, p_20597_, p_20598_, p_20599_);
        } finally {
            ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(null);
        }
    }

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
        at = @At(value = "HEAD"))
    private void felt$spawnReason(ServerLevel worldIn, ItemStack p_20594_, LivingEntity p_20595_, BlockPos p_20596_, EntitySpawnReason p_20597_, boolean p_20598_, boolean p_20599_, CallbackInfoReturnable<T> cir) {
        CreatureSpawnEvent.SpawnReason spawnReason = ((IWorldWriterBridge) worldIn).bridge$getAddEntityReason();
        if (spawnReason == null) {
            ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        }
    }

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
        at = @At("HEAD"), cancellable = true, require = 1, expect = 1, allow = 1)
    private void felt$preCreatureSpawn(ServerLevel level, PostSpawnProcessor<T> consumer, BlockPos pos, EntitySpawnReason reason, boolean tryMoveDown, boolean movedUp, CallbackInfoReturnable<T> cir) {
        CreatureSpawnEvent.SpawnReason spawnReason = ((IWorldWriterBridge) level).bridge$getAddEntityReason();
        if (!felt$callPreCreatureSpawn(level, pos, spawnReason == null ? CreatureSpawnEvent.SpawnReason.DEFAULT : spawnReason)) {
            cir.setReturnValue(null);
        }
    }

    @Unique
    private boolean felt$callPreCreatureSpawn(ServerLevel level, BlockPos pos, CreatureSpawnEvent.SpawnReason spawnReason) {
        // Only the spawn entry points dispatch; direct create callers own their pre-spawn events.
        return new PreCreatureSpawnEvent(CraftLocation.toBukkit(pos, ((WorldBridge) level).bridge$getWorld()),
            CraftEntityType.minecraftToBukkit((EntityType<?>) (Object) this), spawnReason).callEvent();
    }

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V")))
    private void felt$returnIfSuccess(ServerLevel serverLevel, PostSpawnProcessor<T> consumer, BlockPos blockPos, EntitySpawnReason mobSpawnType, boolean bl, boolean bl2, CallbackInfoReturnable<T> cir, T t) {
        if (t != null) {
            cir.setReturnValue(t.isRemoved() ? null : t);
        }
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;playAmbientSound()V"), require = 1, expect = 1, allow = 1)
    private void felt$ambientUnlessRemoved(net.minecraft.world.entity.Mob mob, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (!mob.isRemoved()) {
            original.call(mob);
        }
    }

    public T spawn(ServerLevel p_262634_, BlockPos p_262707_, EntitySpawnReason p_262597_, CreatureSpawnEvent.SpawnReason spawnReason) {
        return this.spawn(p_262634_, null, p_262707_, p_262597_, false, false, spawnReason);
    }

    public T spawn(ServerLevel p_262704_, @Nullable PostSpawnProcessor<T> p_262621_, BlockPos p_262672_, EntitySpawnReason p_262644_, boolean p_262690_, boolean p_262590_, CreatureSpawnEvent.SpawnReason spawnReason) {
        if (!felt$callPreCreatureSpawn(p_262704_, p_262672_, spawnReason)) {
            return null;
        }
        T t = this.create(p_262704_, p_262621_, p_262672_, p_262644_, p_262690_, p_262590_);
        if (t != null) {
            ((IWorldWriterBridge) p_262704_).bridge$pushAddEntityReason(spawnReason);
            p_262704_.addFreshEntityWithPassengers(t);
            return t.isRemoved() ? null : t;
        }
        return null;
    }

    @Override
    public T bridge$spawnCreature(ServerLevel worldIn, BlockPos pos, EntitySpawnReason mobSpawnType, CreatureSpawnEvent.SpawnReason spawnReason) {
        return spawn(worldIn, pos, mobSpawnType, spawnReason);
    }
}
