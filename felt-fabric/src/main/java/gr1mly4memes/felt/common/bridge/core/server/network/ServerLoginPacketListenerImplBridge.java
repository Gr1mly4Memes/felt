package gr1mly4memes.felt.common.bridge.core.server.network;

import com.mojang.authlib.GameProfile;
import gr1mly4memes.felt.common.mod.util.FeltCustomQueryAnswerPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;

public interface ServerLoginPacketListenerImplBridge {
    default Thread bridge$newHandleThread(String name, Runnable runnable) {
        return new Thread(runnable, name);
    }

    int bridge$getVelocityLoginId();

    @javax.annotation.Nullable java.util.UUID bridge$getRequestedUuid();

    @javax.annotation.Nullable String bridge$getRequestedUsername();

    @javax.annotation.Nullable GameProfile bridge$getAuthenticatedProfile();

    void bridge$preLogin(GameProfile authenticatedProfile) throws Exception;

    void bridge$disconnect(String reason);

    default FriendlyByteBuf arclight$platform$customQAData(ServerboundCustomQueryAnswerPacket packet) {
        return FeltCustomQueryAnswerPayload.tryUnwrap(packet.payload());
    }

    default void arclight$platform$onCustomQA(ServerboundCustomQueryAnswerPacket payload) {}
}
