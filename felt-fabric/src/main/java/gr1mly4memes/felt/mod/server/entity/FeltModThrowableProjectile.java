package gr1mly4memes.felt.mod.server.entity;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftThrowableProjectile;

public class FeltModThrowableProjectile extends CraftThrowableProjectile {

    public FeltModThrowableProjectile(CraftServer server, ThrowableItemProjectile entity) {
        super(server, entity);
    }
}
