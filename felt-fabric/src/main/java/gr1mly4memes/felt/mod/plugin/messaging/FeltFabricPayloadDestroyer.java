package gr1mly4memes.felt.mod.plugin.messaging;

import gr1mly4memes.felt.mod.plugin.messaging.FeltPluginChannel;
import gr1mly4memes.felt.mod.plugin.messaging.FeltRawPayload;
import gr1mly4memes.felt.mod.plugin.messaging.PayloadDestroyer;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public record FeltFabricPayloadDestroyer(FeltPluginChannel<FeltFabricPayloadDestroyer> channel) implements FabricPayloadHandler, PayloadDestroyer {

    @Override
    public void receive(FeltRawPayload payload, ServerPlayNetworking.Context context) {}

    @Override
    public void receive(FeltRawPayload payload, ServerConfigurationNetworking.Context context) {}
}
