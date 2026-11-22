package gr1mly4memes.felt.bridge.bukkit;

import gr1mly4memes.felt.mod.plugin.messaging.FeltPluginChannel;
import gr1mly4memes.felt.mod.plugin.messaging.PacketRecorder;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.resources.Identifier;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Set;

public interface MessengerBridge {
    Object2BooleanOpenHashMap<String> valid = new Object2BooleanOpenHashMap<>();

    FeltPluginChannel<?> felt$setupChannel(Identifier channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing);

    void felt$sendCustomPayload(Plugin src, CraftPlayer dst, Identifier location, byte[] data);
    void felt$registerAnonymousOutgoing(Identifier location);
    FeltPluginChannel<?> felt$getAndCheckCrossSend(Plugin src, Identifier channel);
    void felt$checkUnsafeSend(Plugin src, Identifier channel);

    PacketRecorder felt$getPacketRecorder();
}
