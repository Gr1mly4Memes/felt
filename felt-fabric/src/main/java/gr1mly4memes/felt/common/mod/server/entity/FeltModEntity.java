package gr1mly4memes.felt.common.mod.server.entity;

import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;

public class FeltModEntity extends CraftEntity {

    public FeltModEntity(CraftServer server, Entity entity) {
        super(server, entity);
    }
}
