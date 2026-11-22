package gr1mly4memes.felt.bridge.core.world.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/** Internal bridge for LivingEntity-target TemptGoal consumers while preserving Player field ABI. */
public interface TemptGoalBridge {

    @Nullable LivingEntity felt$getEffectiveTarget();

    boolean felt$baseCanScare();

    void felt$runTemptTick();

    int felt$adjustedTickDelay(int ticks);

    Mob felt$getTemptMob();

    double felt$getTemptSpeedModifier();

    void felt$navigateTowardsLiving(LivingEntity target);
}
