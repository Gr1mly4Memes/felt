package gr1mly4memes.felt.common.mod.plugin.messaging;

import gr1mly4memes.felt.common.mod.server.FeltServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface PayloadDestroyer extends PluginChannelHandler {

    default void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        FeltServer.LOGGER.debug("Ignoring sendCustomPayload for channel {} due to conflict with mod channel.", channel().getChannel());
    }

    @Override
    default void updateChannel() {}
}
