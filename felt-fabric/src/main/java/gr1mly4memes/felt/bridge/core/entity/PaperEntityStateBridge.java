package gr1mly4memes.felt.bridge.core.entity;

import net.kyori.adventure.util.TriState;
import org.bukkit.Location;

public interface PaperEntityStateBridge {
    boolean bridge$freezeLocked();
    void bridge$setFreezeLocked(boolean locked);
    boolean bridge$burnInDay();
    void bridge$setBurnInDay(boolean burn);
    TriState bridge$visualFire();
    void bridge$setVisualFire(TriState state);
    Location bridge$origin();
    void bridge$captureOrigin();
    boolean bridge$fromSpawner();
    void bridge$markFromSpawner();
    TriState bridge$despawnInPeaceful();
    void bridge$setDespawnInPeaceful(TriState state);
    TriState bridge$frictionState();
    void bridge$setFrictionState(TriState state);
    boolean bridge$canMobPickup();
    void bridge$setCanMobPickup(boolean pickup);
}
