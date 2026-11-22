package gr1mly4memes.felt.mixin.core.world.entity.ai.goal;

import gr1mly4memes.felt.bridge.core.world.entity.ai.goal.TemptGoalBridge;
import gr1mly4memes.felt.bridge.core.world.entity.ai.goal.ForNonPathfindersTemptBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.world.entity.ai.goal.TemptGoal$ForNonPathfinders")
public abstract class TemptGoal_ForNonPathfindersMixin implements ForNonPathfindersTemptBridge {

    // Overrides the LivingEntity bridge introduced by TemptGoalMixin. The original
    // navigateTowards(Player) override remains untouched and keeps its virtual path.
    @Unique
    public void felt$navigateTowardsLivingForNonPathfinder(LivingEntity target) {
        TemptGoalBridge goal = (TemptGoalBridge) (Object) this;
        Vec3 origin = goal.felt$getTemptMob().position();
        Vec3 point = target.getEyePosition().subtract(origin).scale(goal.felt$getTemptMob().getRandom().nextDouble()).add(origin);
        goal.felt$getTemptMob().getMoveControl().setWantedPosition(point.x, point.y, point.z, goal.felt$getTemptSpeedModifier());
    }
}
