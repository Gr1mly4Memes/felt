package gr1mly4memes.felt.bridge.core.world.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;

/** LivingEntity navigation dispatch specialized for native non-pathfinding TemptGoal subclasses. */
public interface ForNonPathfindersTemptBridge {
    void felt$navigateTowardsLivingForNonPathfinder(LivingEntity target);
}
