package gr1mly4memes.felt.mixin.core.world.entity.animal;

import gr1mly4memes.felt.bridge.core.world.entity.MobBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.animal.panda.Panda$PandaHurtByTargetGoal")
public class Panda_HurtByTargetGoalMixin {

    @Inject(method = "alertOther", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void felt$reason(Mob mobIn, LivingEntity targetIn, CallbackInfo ci) {
        ((MobBridge) mobIn).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY, true);
    }
}
