package gr1mly4memes.felt.bridge.core.server;

import java.util.Collection;
import net.minecraft.world.scores.PlayerTeam;

public interface ServerScoreboardBridge {
    boolean bridge$addPlayersToTeam(Collection<String> players, PlayerTeam team);
    void bridge$removePlayersFromTeam(Collection<String> players, PlayerTeam team);

    default boolean addPlayersToTeam(Collection<String> players, PlayerTeam team) {
        return this.bridge$addPlayersToTeam(players, team);
    }

    default void removePlayersFromTeam(Collection<String> players, PlayerTeam team) {
        this.bridge$removePlayersFromTeam(players, team);
    }

}
