package gr1mly4memes.felt.common.mod.server.entity;

import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractWindCharge;

public class FeltModWindCharge extends CraftAbstractWindCharge {

    public FeltModWindCharge(CraftServer server, AbstractWindCharge entity) {
        super(server, entity);
    }
}
