package gr1mly4memes.felt.mod.util;

import gr1mly4memes.felt.bridge.core.network.ConnectionBridge;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import java.net.InetSocketAddress;

public final class ProxyProtocolHandler extends SimpleChannelInboundHandler<HAProxyMessage> {
    private final ConnectionBridge connection;

    public ProxyProtocolHandler(ConnectionBridge connection) {
        this.connection = connection;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, HAProxyMessage message) {
        if (message.command() == HAProxyCommand.PROXY && message.sourceAddress() != null && message.destinationAddress() != null) {
            this.connection.bridge$setHAProxyAddresses(new InetSocketAddress(message.sourceAddress(), message.sourcePort()),
                new InetSocketAddress(message.destinationAddress(), message.destinationPort()));
        }
        context.pipeline().remove(this);
    }
}
