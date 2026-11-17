package gr1mly4memes.felt.fabric.mod.plugin.messaging;

import gr1mly4memes.felt.common.mod.plugin.messaging.FeltRawPayload;
import gr1mly4memes.felt.common.mod.plugin.messaging.PluginChannelHandler;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public interface FabricPayloadHandler extends PluginChannelHandler, ServerPlayNetworking.PlayPayloadHandler<FeltRawPayload>, ServerConfigurationNetworking.ConfigurationPacketHandler<FeltRawPayload> {}
