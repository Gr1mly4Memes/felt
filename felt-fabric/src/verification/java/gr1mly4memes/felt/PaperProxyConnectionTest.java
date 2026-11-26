package gr1mly4memes.felt;

import com.mojang.authlib.GameProfile;
import gr1mly4memes.felt.bridge.core.network.ConnectionBridge;
import io.netty.channel.ChannelFuture;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.connection.PaperPlayerGameConnection;
import io.papermc.paper.connection.PaperPlayerLoginConnection;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.key.Key;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

final class PaperProxyConnectionTest {
    static void verify(MinecraftServer server) throws Exception {
        var channels = ServerConnectionListener.class.getDeclaredField("channels");
        channels.setAccessible(true);
        var listening = (ChannelFuture) ((List<?>) channels.get(server.getConnection())).getFirst();
        int port = ((InetSocketAddress) listening.channel().localAddress()).getPort();
        var config = GlobalConfiguration.get().proxies;
        boolean previous = config.proxyProtocol;
        var probe = Key.key("felt", "proxy_verification");
        try {
            config.proxyProtocol = false;
            var direct = observeConnection(probe);
            try (var socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
                var connection = direct.get(5, TimeUnit.SECONDS);
                var login = new PaperPlayerLoginConnection(new ServerLoginPacketListenerImpl(server, connection, false));
                check(login.getHAProxyAddress() == null, "A direct socket reported an HAProxy destination");
            }

            config.proxyProtocol = true;
            var proxied = observeConnection(probe);
            try (var socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
                // Use documentation-only addresses to distinguish the header from the physical socket.
                socket.getOutputStream().write("PROXY TCP4 192.0.2.7 198.51.100.8 57214 25565\r\n".getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
                var connection = proxied.get(5, TimeUnit.SECONDS);
                var bridge = (ConnectionBridge) connection;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (bridge.bridge$getHAProxyAddress() == null && System.nanoTime() < deadline) {
                    Thread.sleep(5);
                }
                var destination = new InetSocketAddress("198.51.100.8", 25565);
                check(destination.equals(bridge.bridge$getHAProxyAddress())
                    && new InetSocketAddress("192.0.2.7", 57214).equals(connection.getRemoteAddress())
                    && bridge.bridge$getRawAddress() instanceof InetSocketAddress raw && raw.getAddress().isLoopbackAddress(),
                    "The TCP pipeline did not keep the client, proxy destination and socket peer distinct");
                var login = new PaperPlayerLoginConnection(new ServerLoginPacketListenerImpl(server, connection, false));
                var profile = new GameProfile(UUID.randomUUID(), "ProxyFixture");
                var player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
                player.connection = new ServerGamePacketListenerImpl(server, connection, player, CommonListenerCookie.createInitial(profile, false));
                check(destination.equals(login.getHAProxyAddress())
                    && destination.equals(new PaperPlayerGameConnection(player.connection).getHAProxyAddress())
                    && destination.equals(player.getBukkitEntity().getHAProxyAddress()),
                    "The public login, play and Bukkit player APIs disagreed on the proxy destination");
            }
        } finally {
            config.proxyProtocol = previous;
            ChannelInitializeListenerHolder.removeListener(probe);
        }
        System.out.println("Paper connection API: HAProxy addresses and channel initialization passed over TCP");
    }

    private static CompletableFuture<Connection> observeConnection(Key probe) {
        var result = new CompletableFuture<Connection>();
        ChannelInitializeListenerHolder.addListener(probe, channel -> {
            Connection connection = channel.pipeline().get(Connection.class);
            if (connection == null) {
                result.completeExceptionally(new AssertionError("Paper channel callback ran before the packet handler was installed"));
            } else {
                result.complete(connection);
            }
        });
        return result;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
