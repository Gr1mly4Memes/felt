package gr1mly4memes.felt.bridge.core.world.level;

import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public interface GameRules_ValueBridge<T> {
    void felt$setFrom(T t, @Nullable ServerLevel level);
    void felt$set(Object value, @Nullable ServerLevel level);
}
