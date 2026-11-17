package gr1mly4memes.felt.common.mod.plugin.messaging;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface PluginChannelHandler {
    FeltPluginChannel<?> channel();
    void updateChannel();
    void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data);
}
