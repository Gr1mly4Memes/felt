package gr1mly4memes.felt.common.bridge.bukkit;

import gr1mly4memes.felt.common.mod.plugin.messaging.FeltPluginChannel;
import gr1mly4memes.felt.common.mod.plugin.messaging.PacketRecorder;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.resources.Identifier;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Set;

public interface MessengerBridge {
    Object2BooleanOpenHashMap<String> valid = new Object2BooleanOpenHashMap<>();

    FeltPluginChannel<?> arclight$setupChannel(Identifier channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing);

    void arclight$sendCustomPayload(Plugin src, CraftPlayer dst, Identifier location, byte[] data);
    void arclight$registerAnonymousOutgoing(Identifier location);
    FeltPluginChannel<?> arclight$getAndCheckCrossSend(Plugin src, Identifier channel);
    void arclight$checkUnsafeSend(Plugin src, Identifier channel);

    PacketRecorder arclight$getPacketRecorder();
}
