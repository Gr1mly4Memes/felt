package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.bridge.core.world.level.portal.DimensionTransitionBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import org.bukkit.PortalType;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndGatewayBlock.class)
public class EndGatewayBlockMixin {
    @Inject(method = "entityInside", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;setAsInsidePortal(Lnet/minecraft/world/level/block/Portal;Lnet/minecraft/core/BlockPos;)V"),
        cancellable = true)
    private void felt$enterGateway(BlockState state, Level level, BlockPos pos, Entity entity,
                                    InsideBlockEffectApplier effects, boolean precise, CallbackInfo ci) {
        var event = new EntityPortalEnterEvent(entity.bridge$getBukkitEntity(), CraftLocation.toBukkit(pos, level.bridge$getWorld()),
            PortalType.END_GATEWAY);
        if (!event.callEvent()) {
            ci.cancel();
        }
    }

    @Inject(method = "getPortalDestination", at = @At("RETURN"))
    private void felt$setCause(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<TeleportTransition> cir) {
        var dimensionTransition = cir.getReturnValue();
        if (dimensionTransition != null) {
            ((DimensionTransitionBridge) ((Object) dimensionTransition)).bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause.END_GATEWAY);
        }
    }
}
