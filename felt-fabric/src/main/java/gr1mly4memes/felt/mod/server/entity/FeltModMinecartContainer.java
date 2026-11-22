package gr1mly4memes.felt.mod.server.entity;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMinecartContainer;

public class FeltModMinecartContainer extends CraftMinecartContainer {

    public FeltModMinecartContainer(CraftServer server, AbstractMinecartContainer entity) {
        super(server, entity);
    }
}
