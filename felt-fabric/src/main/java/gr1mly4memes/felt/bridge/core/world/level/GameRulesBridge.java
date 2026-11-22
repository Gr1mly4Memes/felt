package gr1mly4memes.felt.bridge.core.world.level;

import net.minecraft.world.level.gamerules.GameRule;

import java.util.Set;

public interface GameRulesBridge {
    Set<GameRule<?>> felt$getAllRules();

    <T> void bridge$setWorldRule(GameRule<T> rule, T value, net.minecraft.server.level.ServerLevel level);
}
