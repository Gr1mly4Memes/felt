package gr1mly4memes.felt.mixin.core.server.network;

import gr1mly4memes.felt.bridge.core.network.ConnectionBridge;
import gr1mly4memes.felt.mod.util.ProxyProtocolHandler;
import io.netty.channel.Channel;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import java.util.Locale;
import net.minecraft.network.Connection;
import net.minecraft.network.HandlerNames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.network.ServerConnectionListener$1")
public abstract class TcpConnectionInitializerMixin {
    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("RETURN"))
    private void felt$initializePaperChannel(Channel channel, CallbackInfo ci) {
        channel.attr(PaperAdventure.LOCALE_ATTRIBUTE).setIfAbsent(Locale.US);
        var config = GlobalConfiguration.get();
        if (config != null && config.proxies.proxyProtocol) {
            var pipeline = channel.pipeline();
            var connection = (ConnectionBridge) pipeline.get(Connection.class);
            pipeline.addAfter(HandlerNames.TIMEOUT, "haproxy-decoder", new HAProxyMessageDecoder());
            pipeline.addAfter("haproxy-decoder", "haproxy-handler", new ProxyProtocolHandler(connection));
        }
        ChannelInitializeListenerHolder.callListeners(channel);
    }
}
