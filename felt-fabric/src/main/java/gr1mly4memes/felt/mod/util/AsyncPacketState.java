package gr1mly4memes.felt.mod.util;

import java.util.concurrent.CompletableFuture;

/** Completion publishes all preceding packet-buffer writes to the sending thread. */
public final class AsyncPacketState {
    private volatile CompletableFuture<Void> completion = CompletableFuture.completedFuture(null);

    public CompletableFuture<Void> completion() { return this.completion; }

    public boolean isReady() {
        var current = this.completion;
        return current.isDone() && !current.isCompletedExceptionally();
    }

    /** Pending must be set before the packet is published to a connection. */
    public void setReady(boolean ready) {
        if (ready) this.completion.complete(null);
        else this.completion = new CompletableFuture<>();
    }

    public void fail(Throwable failure) { this.completion.completeExceptionally(failure); }
}
