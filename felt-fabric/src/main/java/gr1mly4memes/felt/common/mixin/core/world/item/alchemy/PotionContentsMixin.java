package gr1mly4memes.felt.common.mixin.core.world.item.alchemy;

import gr1mly4memes.felt.common.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** PotionContents.ConsumableListener is the ordinary potion-drink add-effect path. */
@Mixin(PotionContents.class)
public class PotionContentsMixin {

    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean arclight$potionContentsEffect(LivingEntity instance, MobEffectInstance effect) throws Throwable {
        ((LivingEntityBridge) instance).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.POTION_DRINK);
        return (boolean) DecorationOps.callsite().invoke(instance, effect);
    }
}
