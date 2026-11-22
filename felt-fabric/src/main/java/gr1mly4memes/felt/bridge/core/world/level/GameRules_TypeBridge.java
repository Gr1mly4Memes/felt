package gr1mly4memes.felt.bridge.core.world.level;

import net.minecraft.server.level.ServerLevel;

import java.util.function.BiConsumer;

public interface GameRules_TypeBridge<T> {
    void felt$setPerWorldCallback(BiConsumer<ServerLevel, T> callback);
    void felt$runCallback(ServerLevel level, T value);
}
