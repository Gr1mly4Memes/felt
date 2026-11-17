package gr1mly4memes.felt.common.mod.server.entity;

import net.minecraft.world.entity.Mob;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMob;

public class FeltModMob extends CraftMob {

    public FeltModMob(CraftServer server, Mob entity) {
        super(server, entity);
    }
}
