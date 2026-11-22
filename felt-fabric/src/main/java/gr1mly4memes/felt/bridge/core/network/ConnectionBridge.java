package gr1mly4memes.felt.bridge.core.network;

import com.mojang.authlib.properties.Property;
import gr1mly4memes.felt.mod.util.ConnectionCookieRequests;
import gr1mly4memes.felt.mod.util.ConnectionResourcePackCallbacks;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;
import java.util.UUID;

public interface ConnectionBridge {

    SocketAddress bridge$getRawAddress();

    int bridge$getProtocolVersion();

    java.net.InetSocketAddress bridge$getVirtualHost();

    void bridge$setHandshakeMetadata(int protocolVersion, java.net.InetSocketAddress virtualHost);

    @Nullable
    org.bukkit.event.player.PlayerResourcePackStatusEvent.Status bridge$getResourcePackStatus();

    void bridge$setResourcePackStatus(org.bukkit.event.player.PlayerResourcePackStatusEvent.Status status);

    @Nullable
    String bridge$getClientBrandName();

    void bridge$setClientBrandName(@Nullable String brand);

    ConnectionCookieRequests bridge$getCookieRequests();

    ConnectionResourcePackCallbacks bridge$getResourcePackCallbacks();

    UUID bridge$getSpoofedUUID();

    void bridge$setSpoofedUUID(UUID spoofedUUID);

    Property[] bridge$getSpoofedProfile();

    void bridge$setSpoofedProfile(Property[] spoofedProfile);

    String bridge$getHostname();

    void bridge$setHostname(String hostname);
}
