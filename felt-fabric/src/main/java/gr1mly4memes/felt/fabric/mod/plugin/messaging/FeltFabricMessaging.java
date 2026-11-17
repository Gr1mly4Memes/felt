package gr1mly4memes.felt.fabric.mod.plugin.messaging;

import gr1mly4memes.felt.common.mod.server.FeltServer;
import gr1mly4memes.felt.common.mod.plugin.messaging.FeltPluginChannel;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.resources.Identifier;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeltFabricMessaging {

    public static FeltPluginChannel<? extends FabricPayloadHandler> setupChannel(Messenger messenger, Identifier location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        if (verifyChannel(location, incoming, outgoing)) {
            var channel = new FeltPluginChannel<>(messenger, FeltFabricPayloadHandler::new, location, incoming, outgoing);
            PayloadTypeRegistry.clientboundConfiguration().register(channel.getType(), channel.getStreamCodec());
            PayloadTypeRegistry.serverboundConfiguration().register(channel.getType(), channel.getStreamCodec());
            PayloadTypeRegistry.clientboundPlay().register(channel.getType(), channel.getStreamCodec());
            PayloadTypeRegistry.serverboundPlay().register(channel.getType(), channel.getStreamCodec());
            ServerPlayNetworking.registerGlobalReceiver(channel.getType(), channel.getChannelHandler());
            return channel;
        } else {
            return new FeltPluginChannel<>(messenger, FeltFabricPayloadDestroyer::new, location, incoming, outgoing);
        }
    }

    private static boolean verifyChannel(Identifier location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        for (var protocol : FeltPluginChannel.PROTOCOLS) {
            var s2c = PayloadTypeRegistryImpl.CLIENTBOUND_PLAY.get(location);
            if (s2c != null) {
                var pluginList = Stream.concat(outgoing.stream(), incoming.stream().map(PluginMessageListenerRegistration::getPlugin))
                        .distinct()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                FeltServer.LOGGER.error("Attempting to register a channel that has already been registered by Fabric!");
                FeltServer.LOGGER.error("Channel conflict: {}, in protocol: {}", location, protocol);
                FeltServer.LOGGER.error("Registered by plugin(s): {}", pluginList);
                FeltServer.LOGGER.error("This channel will be ignored for the rest of the time!");
                return false;
            }
        }
        return true;
    }
}
