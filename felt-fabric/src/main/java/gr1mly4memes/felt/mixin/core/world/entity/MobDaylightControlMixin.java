package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobDaylightControlMixin {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "checkDespawn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;isAllowedInPeaceful()Z"))
    private boolean felt$peacefulOverride(boolean allowed) {
        return !((PaperEntityStateBridge)this).bridge$despawnInPeaceful().toBooleanOrElse(!allowed);
    }
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void felt$daylightBurning(CallbackInfoReturnable<Boolean> cir) {
        if (!((PaperEntityStateBridge)this).bridge$burnInDay()) cir.setReturnValue(false);
    }
}
