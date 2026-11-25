package gr1mly4memes.felt.bridge.core.network;

/**
 * Paper defers sending chunk packets while asynchronous anti-xray rewriting is
 * in progress. Vanilla packets do not expose this state, so Felt supplies it.
 */
public interface ChunkPacketReadyBridge {
    boolean isReady();

    void setReady(boolean ready);

    java.util.concurrent.CompletableFuture<Void> bridge$completion();

    void bridge$fail(Throwable failure);
}
