package gr1mly4memes.felt.common.mixin.core.world.level.block.piston;

import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felt.common.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.common.bridge.core.server.MinecraftServerBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntityActivationMixin {
    @Decorate(method = "moveCollidedEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
    private static void arclight$slimeImmunity(Entity entity, double x, double y, double z) throws Throwable {
        DecorationOps.callsite().invoke(entity, x, y, z);
        if (entity.level().isClientSide()) return;
        EntityBridge bridge = (EntityBridge) entity;
        long tick = ((MinecraftServerBridge) entity.level().getServer()).bridge$getCurrentTick();
        bridge.bridge$setActivatedTick(Math.max(bridge.bridge$getActivatedTick(), tick + 10));
        bridge.bridge$setActivatedImmunityTick(Math.max(bridge.bridge$getActivatedImmunityTick(), tick + 10));
    }
}
