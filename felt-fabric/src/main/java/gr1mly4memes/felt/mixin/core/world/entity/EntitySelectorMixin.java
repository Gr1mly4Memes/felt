package gr1mly4memes.felt.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    @WrapOperation(method = "lambda$pushableBy$0(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/scores/Team;Lnet/minecraft/world/scores/Team$CollisionRule;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isPushable()Z"),
        require = 1, expect = 1, allow = 1)
    private static boolean felt$bothCollisionRules(Entity candidate, Operation<Boolean> original,
            Entity subject, Team ownTeam, Team.CollisionRule ownRule, Entity input) {
        if (!((EntityBridge) candidate).bridge$canCollideWithBukkit(subject, original.call(candidate))) {
            return false;
        }
        return ((EntityBridge) subject).bridge$canCollideWithBukkit(candidate, subject.isPushable());
    }
}
