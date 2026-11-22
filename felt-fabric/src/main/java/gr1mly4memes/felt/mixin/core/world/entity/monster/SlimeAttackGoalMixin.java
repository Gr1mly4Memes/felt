package gr1mly4memes.felt.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.SlimeTargetLivingEntityEvent;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobAttackGoal")
public abstract class SlimeAttackGoalMixin extends Goal {

    @Shadow @Final private AbstractCubeMob cubeMob;
    @Shadow private int growTiredTimer;

    @Decorate(method = {"canUse()Z", "canContinueToUse()Z"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/cubemob/AbstractCubeMob;canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z"),
        require = 2, expect = 2, allow = 2)
    private boolean felt$liveTarget(AbstractCubeMob cubeMob, LivingEntity target) throws Throwable {
        return target.isAlive() && (boolean) DecorationOps.callsite().invoke(cubeMob, target);
    }

    @Decorate(method = {"canUse()Z", "canContinueToUse()Z"}, at = @At("RETURN"))
    private void felt$targetEvent(boolean canUse, @Local(ordinal = 0) LivingEntity target) throws Throwable {
        // Keep the native move-control/tired-timer gates before dispatch.
        if (canUse) {
            SlimeTargetLivingEntityEvent event = new SlimeTargetLivingEntityEvent(
                (org.bukkit.entity.AbstractCubeMob) ((EntityBridge) this.cubeMob).bridge$getBukkitEntity(),
                ((LivingEntityBridge) target).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            canUse = !event.isCancelled();
        }
        DecorationOps.callsite().invoke(canUse);
    }

    // The native goal inherits the empty Goal.stop(); Paper resets both here.
    @Override
    public void stop() {
        this.growTiredTimer = 0;
        this.cubeMob.setTarget(null);
    }
}
