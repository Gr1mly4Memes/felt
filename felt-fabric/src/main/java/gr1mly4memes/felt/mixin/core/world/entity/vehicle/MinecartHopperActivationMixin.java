package gr1mly4memes.felt.mixin.core.world.entity.vehicle;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartHopper.class)
public abstract class MinecartHopperActivationMixin {
    private void felt$immunize() {
        var entity = (MinecartHopper) (Object) this;
        if (entity.level().isClientSide()) return;
        EntityBridge bridge = (EntityBridge) entity;
        long tick = ((MinecraftServerBridge) entity.level().getServer()).bridge$getCurrentTick();
        bridge.bridge$setActivatedImmunityTick(Math.max(bridge.bridge$getActivatedImmunityTick(), tick + 20));
    }
    @Inject(method = "activateMinecart", at = @At("RETURN"))
    private void felt$activation(CallbackInfo ci) { this.felt$immunize(); }
    @Inject(method = "suckInItems", at = @At("RETURN"))
    private void felt$pickup(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) this.felt$immunize();
    }
}
