package gr1mly4memes.felt.bridge.core.world.level.levelgen.structure;

import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.event.world.AsyncStructureGenerateEvent;

public interface StructureStartBridge {

    void bridge$setGenerateCause(AsyncStructureGenerateEvent.Cause cause);

    CraftPersistentDataContainer bridge$getPersistentDataContainer();
}
