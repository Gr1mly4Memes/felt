package gr1mly4memes.felt.mixin.core.world.entity.projectile.hurtingprojectile.windcharge;

import gr1mly4memes.felt.mixin.core.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectileMixin;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWindCharge.class)
public abstract class AbstractWindChargeMixin extends AbstractHurtingProjectileMixin {

    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/windcharge/AbstractWindCharge;explode(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void felt$hitBlock(BlockHitResult blockHitResult, CallbackInfo ci) {
        bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/windcharge/AbstractWindCharge;discard()V"))
    private void felt$hit(HitResult hitResult, CallbackInfo ci) {
        bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/windcharge/AbstractWindCharge;explode(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void felt$outOfWorld(CallbackInfo ci) {
        bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.OUT_OF_WORLD);
    }
}
