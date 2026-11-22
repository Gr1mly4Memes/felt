package gr1mly4memes.felt.bridge.bukkit;

import org.bukkit.event.entity.EntityDamageEvent;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;

public interface EntityDamageEventBridge {
    EntityDamageEvent.DamageModifier[] VANILLA_VALUES = new EntityDamageEvent.DamageModifier[] {
            BASE, BLOCKING, FREEZING, HARD_HAT, ARMOR, RESISTANCE, MAGIC, ABSORPTION
    };
    boolean felt$applicable(EntityDamageEvent.DamageModifier stage);
    double felt$applyOriginal(EntityDamageEvent.DamageModifier currentStage, double lastStage);
    boolean felt$isStillOriginal(EntityDamageEvent.DamageModifier modifier, double last, double current);
}
