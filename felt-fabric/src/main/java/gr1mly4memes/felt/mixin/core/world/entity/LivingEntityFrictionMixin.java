package gr1mly4memes.felt.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFrictionMixin {
    @ModifyReturnValue(method = "shouldDiscardFriction", at = @At("RETURN"))
    private boolean felt$frictionOverride(boolean discard) {
        return !((PaperEntityStateBridge)this).bridge$frictionState().toBooleanOrElse(!discard);
    }
}
