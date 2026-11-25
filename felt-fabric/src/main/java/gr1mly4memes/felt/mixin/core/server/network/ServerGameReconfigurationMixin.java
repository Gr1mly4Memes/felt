package gr1mly4memes.felt.mixin.core.server.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.server.network.ServerConfigurationPacketListenerImplBridge;
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGameReconfigurationMixin {
    @WrapOperation(method = "handleConfigurationAcknowledged", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/Connection;setupInboundProtocol(Lnet/minecraft/network/ProtocolInfo;Lnet/minecraft/network/PacketListener;)V"))
    private void felt$fireReconfigure(Connection connection, ProtocolInfo<?> protocol, PacketListener listener, Operation<Void> original) {
        original.call(connection, protocol, listener);
        if (listener instanceof ServerConfigurationPacketListenerImpl configuration) {
            var api = ((ServerConfigurationPacketListenerImplBridge) configuration).bridge$getConfigurationConnection();
            new PlayerConnectionReconfigureEvent(api).callEvent();
        }
    }
}
