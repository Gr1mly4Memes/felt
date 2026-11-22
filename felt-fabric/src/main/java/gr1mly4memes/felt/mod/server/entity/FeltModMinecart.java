package gr1mly4memes.felt.mod.server.entity;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMinecart;

public class FeltModMinecart extends CraftMinecart {

    public FeltModMinecart(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }
}
