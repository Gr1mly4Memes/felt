package gr1mly4memes.felt.mixin.core.world.effect;

import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.effect.HungerMobEffect")
public class HungerMobEffectMixin {

    @Inject(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void felt$reason(ServerLevel serverLevel, LivingEntity livingEntity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        ((PlayerBridge) livingEntity).bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.HUNGER_EFFECT);
    }
}
