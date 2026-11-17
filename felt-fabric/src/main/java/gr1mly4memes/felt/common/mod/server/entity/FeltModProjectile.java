package gr1mly4memes.felt.common.mod.server.entity;

import net.minecraft.world.entity.projectile.Projectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftProjectile;

public class FeltModProjectile extends CraftProjectile {

    public FeltModProjectile(CraftServer server, Projectile entity) {
        super(server, entity);
    }
}
