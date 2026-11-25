package gr1mly4memes.felt.bridge.core.entity;

import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public interface InitialSpawnReasonBridge {
    SpawnReason bridge$initialSpawnReason();
    void bridge$setInitialSpawnReason(SpawnReason reason);
}
