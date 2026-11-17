package gr1mly4memes.felt.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftVehicle;

public class FeltModVehicle extends CraftVehicle {

    public FeltModVehicle(CraftServer server, VehicleEntity entity) {
        super(server, entity);
    }
}
