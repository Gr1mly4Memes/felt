package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LivingEntity.class)
public abstract class LivingEntityVoidDamageMixin {
    @ModifyConstant(method = "onBelowWorld", constant = @Constant(floatValue = 4.0F), require = 1)
    private float felt$voidDamageAmount(float original) {
        return ((LivingEntity)(Object)this).level() instanceof ServerLevel level
            ? ((WorldBridge)level).bridge$getWorld().getVoidDamageAmount() : original;
    }
}
