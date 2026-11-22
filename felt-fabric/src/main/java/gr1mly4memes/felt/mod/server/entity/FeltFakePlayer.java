package gr1mly4memes.felt.mod.server.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class FeltFakePlayer extends CraftPlayer {

    public FeltFakePlayer(CraftServer server, ServerPlayer entity) {
        super(server, entity);
    }

    @Override
    public boolean isOp() {
        GameProfile profile = this.getHandle().getGameProfile();
        return profile != null && profile.id() != null && super.isOp();
    }

    @Override
    public void setOp(boolean value) {
    }
}
