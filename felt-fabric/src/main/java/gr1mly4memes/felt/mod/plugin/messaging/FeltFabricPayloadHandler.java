package gr1mly4memes.felt.mod.plugin.messaging;

import gr1mly4memes.felt.mod.plugin.messaging.FeltPluginChannel;
import gr1mly4memes.felt.mod.plugin.messaging.FeltRawPayload;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import gr1mly4memes.felt.mod.plugin.messaging.FeltRawPayload;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public record FeltFabricPayloadHandler(FeltPluginChannel<FeltFabricPayloadHandler> channel) implements FabricPayloadHandler {

    @Override
    public void updateChannel() {
    }

    @Override
    public void receive(FeltRawPayload pkt, ServerPlayNetworking.Context ctx) {
        ctx.server().executeIfPossible(() -> {
            var bukkit = ((ServerPlayerBridge)ctx.player()).bridge$getBukkitEntity();
            channel.dispatchMessage(bukkit, pkt.felt$leak());
        });
    }

    @Override
    public void receive(FeltRawPayload pkt, ServerConfigurationNetworking.Context ctx) {
        ctx.server().executeIfPossible(() -> {
            var bukkit = ((ServerCommonPacketListenerImplBridge)ctx.packetListener()).bridge$getCraftPlayer();
            channel.dispatchMessage(bukkit, pkt.felt$leak());
        });
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        var player = dst.getHandle();
        if (player.connection != null) {
            ServerPlayNetworking.send(player, new FeltRawPayload(channel.getType(), data));
        }
    }
}
