package gr1mly4memes.felt.bridge.core.world.level;

import net.minecraft.world.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;

public interface IWorldWriterBridge {

    boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason);

    void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason);

    CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason();
}
