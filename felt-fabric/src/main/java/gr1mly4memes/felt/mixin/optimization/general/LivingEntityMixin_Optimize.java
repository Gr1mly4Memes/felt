package gr1mly4memes.felt.mixin.optimization.general;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Paper - Perf: Use distance squared. Avoids a {@code sqrt} in the line-of-sight
 * distance check (used by {@code CraftLivingEntity#hasLineOfSight} and
 * {@code CraftWorld#lineOfSightExists}).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Optimize {

    @Inject(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z", at = @At("HEAD"), cancellable = true)
    private void felt$hasLineOfSight(Entity target, ClipContext.Block blockCollidingContext, ClipContext.Fluid fluidCollidingContext, double eyeHeight, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        Vec3 from = new Vec3(self.getX(), self.getEyeY(), self.getZ());
        Vec3 to = new Vec3(target.getX(), eyeHeight, target.getZ());
        cir.setReturnValue(!(to.distanceToSqr(from) > Mth.square(128.0))
            && self.level().clip(new ClipContext(from, to, blockCollidingContext, fluidCollidingContext, self)).getType() == HitResult.Type.MISS);
    }
}