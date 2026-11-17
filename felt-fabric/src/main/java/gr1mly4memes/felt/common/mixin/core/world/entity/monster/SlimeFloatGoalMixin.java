package gr1mly4memes.felt.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.SlimeSwimEvent;
import gr1mly4memes.felt.common.bridge.core.entity.EntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.monster.Slime;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeFloatGoal")
public abstract class SlimeFloatGoalMixin {

    @Shadow @Final private Slime slime;

    @Decorate(method = "canUse()Z", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private void arclight$swimEvent(boolean canUse) throws Throwable {
        // Goal.canContinueToUse() delegates to this same canUse().
        if (canUse) {
            SlimeSwimEvent event = new SlimeSwimEvent(
                (org.bukkit.entity.Slime) ((EntityBridge) this.slime).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            canUse = !event.isCancelled();
        }
        DecorationOps.callsite().invoke(canUse);
    }
}
