package gr1mly4memes.felt.common.bridge.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;

import java.util.Map;
import java.util.function.Function;

public interface CommandsBridge {
    interface RestrictedMarker {}

    default com.mojang.brigadier.tree.RootCommandNode<net.minecraft.commands.CommandSourceStack> bridge$mod$filterCommandTree(
            net.minecraft.server.level.ServerPlayer player,
            com.mojang.brigadier.tree.RootCommandNode<net.minecraft.commands.CommandSourceStack> root) {
        return root;
    }

    default <S, T> void bridge$mod$mergeNode(CommandNode<S> sourceNode, CommandNode<T> resultNode,
                                       Map<CommandNode<S>, CommandNode<T>> sourceToResult,
                                       S canUse, Command<T> execute,
                                       Function<SuggestionProvider<S>, SuggestionProvider<T>> sourceToResultSuggestion) {
    }
}
