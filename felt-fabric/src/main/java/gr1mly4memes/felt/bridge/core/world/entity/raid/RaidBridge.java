package gr1mly4memes.felt.bridge.core.world.entity.raid;

import java.util.Collection;
import net.minecraft.world.entity.raid.Raider;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;

public interface RaidBridge {
    int bridge$getId();
    void bridge$setId(int id);
    CraftPersistentDataContainer bridge$getPersistentData();

    Collection<Raider> bridge$getRaiders();
}
