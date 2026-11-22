package gr1mly4memes.felt.mixin.core.world.entity.animal;

import com.destroystokyo.paper.event.entity.TurtleGoHomeEvent;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.animal.turtle.Turtle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleGoHomeGoal")
public abstract class TurtleGoHomeGoalMixin {

    @Shadow @Final private Turtle turtle;

    // Vanilla has four return sites; mod loader merges the random/distance result into one (three sites).
    @Decorate(method = "canUse()Z", at = @At("RETURN"), require = 3, allow = 4)
    private void felt$goHomeEvent(boolean canUse) throws Throwable {
        if (canUse) {
            canUse = new TurtleGoHomeEvent(
                (org.bukkit.entity.Turtle) ((EntityBridge) this.turtle).bridge$getBukkitEntity()).callEvent();
        }
        DecorationOps.callsite().invoke(canUse);
    }
}
