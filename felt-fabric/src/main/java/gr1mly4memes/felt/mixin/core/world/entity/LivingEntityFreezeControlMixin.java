package gr1mly4memes.felt.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFreezeControlMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setTicksFrozen(I)V"))
    private void felt$lockedFreezeTicks(LivingEntity entity, int ticks, Operation<Void> original) {
        if (!((PaperEntityStateBridge)entity).bridge$freezeLocked()) original.call(entity, ticks);
    }
}
