package gr1mly4memes.felt.mixin.core.network;

import com.mojang.authlib.properties.Property;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gr1mly4memes.felt.bridge.core.network.ConnectionBridge;
import gr1mly4memes.felt.mod.util.ConnectionCookieRequests;
import gr1mly4memes.felt.mod.util.ConnectionResourcePackCallbacks;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(Connection.class)
public class ConnectionMixin implements ConnectionBridge {

    @Inject(method = "channelActive", at = @At("HEAD"))
    private void felt$installAsyncPacketGate(io.netty.channel.ChannelHandlerContext context, CallbackInfo ci) {
        // Outbound messages reach this gate before bundle expansion and packet encoding.
        context.pipeline().addBefore(context.name(), "felt_async_packets",
            new gr1mly4memes.felt.mod.util.AsyncPacketSendGate());
    }

    @Shadow public boolean disconnectionHandled;
    @Shadow public io.netty.channel.Channel channel;
    @Shadow public SocketAddress address;
    @Unique private volatile @Nullable InetSocketAddress felt$haProxyAddress;

    @Override
    public @Nullable InetSocketAddress bridge$getHAProxyAddress() {
        return this.felt$haProxyAddress;
    }

    @Override
    public void bridge$setHAProxyAddresses(InetSocketAddress source, InetSocketAddress destination) {
        this.address = source;
        this.felt$haProxyAddress = destination;
    }

    @Override
    public java.net.SocketAddress bridge$getRawAddress() {
        return this.channel.remoteAddress();
    }
    @Shadow @Final private Queue<Consumer<Connection>> pendingActions;
    public java.util.UUID spoofedUUID;
    public com.mojang.authlib.properties.Property[] spoofedProfile;
    public String hostname;
    public int protocolVersion = -1;
    public java.net.InetSocketAddress virtualHost;

    @Override
    public int bridge$getProtocolVersion() {
        return this.protocolVersion;
    }

    @Override
    public java.net.InetSocketAddress bridge$getVirtualHost() {
        return this.virtualHost;
    }

    @Override
    public void bridge$setHandshakeMetadata(int protocolVersion, java.net.InetSocketAddress virtualHost) {
        this.protocolVersion = protocolVersion;
        this.virtualHost = virtualHost;
    }

    public volatile org.bukkit.event.player.PlayerResourcePackStatusEvent.Status resourcePackStatus;

    @Override
    public org.bukkit.event.player.PlayerResourcePackStatusEvent.Status bridge$getResourcePackStatus() {
        return this.resourcePackStatus;
    }

    @Override
    public void bridge$setResourcePackStatus(org.bukkit.event.player.PlayerResourcePackStatusEvent.Status status) {
        this.resourcePackStatus = status;
    }

    @Unique private volatile @Nullable String felt$clientBrandName;

    @Override
    @Nullable
    public String bridge$getClientBrandName() {
        return this.felt$clientBrandName;
    }

    @Override
    public void bridge$setClientBrandName(@Nullable String brand) {
        this.felt$clientBrandName = brand;
    }

    @Unique private final ConnectionCookieRequests felt$cookieRequests = new ConnectionCookieRequests();

    @Override
    public ConnectionCookieRequests bridge$getCookieRequests() {
        return this.felt$cookieRequests;
    }

    @Unique private final ConnectionResourcePackCallbacks felt$resourcePackCallbacks = new ConnectionResourcePackCallbacks();

    @Override
    public ConnectionResourcePackCallbacks bridge$getResourcePackCallbacks() {
        return this.felt$resourcePackCallbacks;
    }

    @Override
    public UUID bridge$getSpoofedUUID() {
        return spoofedUUID;
    }

    @Override
    public void bridge$setSpoofedUUID(UUID spoofedUUID) {
        this.spoofedUUID = spoofedUUID;
    }

    @Override
    public Property[] bridge$getSpoofedProfile() {
        return spoofedProfile;
    }

    @Override
    public void bridge$setSpoofedProfile(Property[] spoofedProfile) {
        this.spoofedProfile = spoofedProfile;
    }

    @Override
    public String bridge$getHostname() {
        return hostname;
    }

    @Override
    public void bridge$setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Redirect(method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;"))
    private ChannelFuture felt$noAwaitClose(ChannelFuture future) {
        // Disconnect can run on the event loop; retain close and the native details tail.
        return future;
    }

    @Inject(method = "channelInactive(Lio/netty/channel/ChannelHandlerContext;)V", at = @At("HEAD"))
    private void felt$closeConnectionRequests(CallbackInfo ci) {
        // Only the native channel's terminal lifecycle, never listener handoff or a cancellable kick.
        this.felt$resourcePackCallbacks.close();
        this.felt$cookieRequests.close();
    }

    @Inject(method = "handleDisconnection", at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;disconnectionHandled:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void felt$markFirstDisconnection(CallbackInfo ci, @Share("felt$firstDisconnection") LocalBooleanRef firstDisconnection) {
        firstDisconnection.set(true);
    }

    @Inject(method = "handleDisconnection", at = @At("RETURN"))
    private void felt$clearDisconnectedQueue(CallbackInfo ci, @Share("felt$firstDisconnection") LocalBooleanRef firstDisconnection) {
        // Only the first closed branch's normal tail; exceptions retain the queue.
        if (firstDisconnection.get()) {
            this.pendingActions.clear();
        }
    }

    @Inject(method = "handleDisconnection", at = @At("HEAD"), cancellable = true)
    private void felt$noDisconnectTwiceWarn(CallbackInfo ci) {
        if (disconnectionHandled) {
            ci.cancel();
        }
    }
}
