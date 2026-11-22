package gr1mly4memes.felt.mixin.core.world.item;

import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ApplyStatusEffectsConsumeEffect.class)
public class PotionItemMixin {

    /** Apply-effects consume entries are FOOD only when their actual stack is food. */
    @Decorate(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean felt$foodConsumeEffect(LivingEntity instance, MobEffectInstance effect,
                                                Level level, ItemStack stack, LivingEntity entity) throws Throwable {
        if (stack.has(DataComponents.FOOD)) {
            ((LivingEntityBridge) instance).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.FOOD);
        }
        return (boolean) DecorationOps.callsite().invoke(instance, effect);
    }
}
