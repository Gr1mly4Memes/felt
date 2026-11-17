package gr1mly4memes.felt.common.mod.server.entity;

import net.minecraft.world.entity.animal.FlyingAnimal;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Flying;

public class FeltModFlying extends FeltModMob implements Flying {

    public FeltModFlying(CraftServer server, FlyingAnimal entity) {
        super(server, (net.minecraft.world.entity.Mob) entity);
    }
}