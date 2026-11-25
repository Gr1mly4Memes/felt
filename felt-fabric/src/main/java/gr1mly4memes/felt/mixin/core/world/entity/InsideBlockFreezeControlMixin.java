package gr1mly4memes.felt.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InsideBlockEffectType.class)
public abstract class InsideBlockFreezeControlMixin {
    @ModifyExpressionValue(method = "lambda$static$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canFreeze()Z"))
    private static boolean felt$lockedPowderSnowTicks(boolean canFreeze, @Local(argsOnly = true) Entity entity) {
        return canFreeze && !((PaperEntityStateBridge)entity).bridge$freezeLocked();
    }
}
