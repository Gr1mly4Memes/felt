package gr1mly4memes.felt.mod.server.entity;

import net.minecraft.world.entity.raid.Raider;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftRaider;

public class FeltModRaider extends CraftRaider {

    public FeltModRaider(CraftServer server, Raider entity) {
        super(server, entity);
    }
}
