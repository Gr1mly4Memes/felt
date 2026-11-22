package gr1mly4memes.felt.bridge.core.server.network.config;

import net.minecraft.server.level.ServerPlayer;

/** Binds this configuration task to its own login player. */
public interface PrepareSpawnTaskBridge {
    void bridge$setSpawnPlayer(ServerPlayer player);
}
