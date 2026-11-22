package gr1mly4memes.felt.bridge.core.server;

public interface ServerTickRateManagerBridge {

    boolean bridge$requestGameToSprint(int time, boolean silent);

    default boolean requestGameToSprint(int time, boolean silent) {
        return this.bridge$requestGameToSprint(time, silent);
    }

}
