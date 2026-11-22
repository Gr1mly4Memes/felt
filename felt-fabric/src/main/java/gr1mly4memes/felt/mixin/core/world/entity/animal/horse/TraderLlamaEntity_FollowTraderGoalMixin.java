package gr1mly4memes.felt.mixin.core.world.entity.animal.horse;

import gr1mly4memes.felt.bridge.core.world.entity.MobBridge;
import gr1mly4memes.felt.mixin.core.world.entity.ai.goal.target.TargetGoalMixin;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("public-target")
@Mixin(targets = "net.minecraft.world.entity.animal.equine.TraderLlama$TraderLlamaDefendWanderingTraderGoal")
public class TraderLlamaEntity_FollowTraderGoalMixin extends TargetGoalMixin {

    @Inject(method = "start", at = @At("HEAD"))
    private void felt$reason(CallbackInfo ci) {
        ((MobBridge) this.mob).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER, true);
    }
}
