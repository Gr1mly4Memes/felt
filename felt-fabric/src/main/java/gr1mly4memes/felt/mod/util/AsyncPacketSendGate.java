package gr1mly4memes.felt.mod.util;

import gr1mly4memes.felt.bridge.core.network.ChunkPacketReadyBridge;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.network.protocol.BundlePacket;

/** Event-loop confined gate, reached before packet encoders during outbound traversal. */
public final class AsyncPacketSendGate extends ChannelDuplexHandler {
    private static final CompletableFuture<Void> READY = CompletableFuture.completedFuture(null);
    private record Write(Object message, ChannelPromise promise, CompletableFuture<Void> completion) {}
    private final ArrayDeque<Write> pending = new ArrayDeque<>();
    private final Function<Object, CompletableFuture<Void>> readiness;
    private CompletableFuture<Void> waiting;
    private boolean closed;

    public AsyncPacketSendGate() { this(AsyncPacketSendGate::completion); }

    public AsyncPacketSendGate(Function<Object, CompletableFuture<Void>> readiness) {
        this.readiness = readiness;
    }

    private static CompletableFuture<Void> completion(Object message) {
        if (message instanceof ChunkPacketReadyBridge packet) return packet.bridge$completion();
        if (message instanceof BundlePacket<?> bundle) {
            var futures = new java.util.ArrayList<CompletableFuture<Void>>();
            for (var packet : bundle.subPackets()) futures.add(completion(packet));
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
        return READY;
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
        if (this.closed) {
            ReferenceCountUtil.release(message);
            promise.tryFailure(new ClosedChannelException());
            return;
        }
        this.pending.addLast(new Write(message, promise, this.readiness.apply(message)));
        drain(context);
    }

    @Override
    public void flush(ChannelHandlerContext context) {
        if (this.closed) return;
        this.pending.addLast(new Write(null, null, READY));
        drain(context);
    }

    private void drain(ChannelHandlerContext context) {
        if (this.closed) return;
        while (!this.pending.isEmpty()) {
            Write next = this.pending.peekFirst();
            if (!next.completion.isDone()) {
                if (this.waiting != next.completion) {
                    this.waiting = next.completion;
                    next.completion.whenComplete((ignored, failure) -> {
                        // Never block Netty or write from an obfuscation worker.
                        context.executor().execute(() -> drain(context));
                    });
                }
                return;
            }
            if (next.completion.isCompletedExceptionally()) {
                next.completion.whenComplete((ignored, failure) -> {
                    discard(failure);
                    context.close();
                    context.fireExceptionCaught(failure);
                });
                return;
            }
            this.waiting = null;
            this.pending.removeFirst();
            if (next.message == null) context.flush();
            else context.write(next.message, next.promise);
        }
    }

    private void discard(Throwable failure) {
        this.closed = true;
        this.waiting = null;
        Write write;
        while ((write = this.pending.pollFirst()) != null) {
            if (write.message != null) {
                ReferenceCountUtil.release(write.message);
                write.promise.tryFailure(failure);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        discard(new ClosedChannelException());
        context.fireChannelInactive();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext context) {
        discard(new ClosedChannelException());
    }
}
