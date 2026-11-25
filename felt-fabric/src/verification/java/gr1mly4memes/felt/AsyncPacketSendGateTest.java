package gr1mly4memes.felt;

import gr1mly4memes.felt.mod.util.AsyncPacketSendGate;
import gr1mly4memes.felt.mod.util.AsyncPacketState;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.IdentityHashMap;
import java.util.concurrent.CompletableFuture;

public final class AsyncPacketSendGateTest {
    private static final class Recorder extends ChannelOutboundHandlerAdapter {
        final java.util.List<Object> writes = new java.util.ArrayList<>();
        int flushes;
        @Override public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
            writes.add(message);
            ctx.write(message, promise);
        }
        @Override public void flush(ChannelHandlerContext ctx) { flushes++; ctx.flush(); }
    }

    public static void main(String[] args) throws Exception {
        orderedCompletion();
        failedObfuscation();
        disconnectWhilePending();
        sharedPacket();
        bundledPacket();
        System.out.println("Async packet gate: ordering, worker completion, flushes, failures, disconnects and shared packets passed");
    }

    private static void orderedCompletion() throws Exception {
        var first = new AsyncPacketState();
        var second = new AsyncPacketState();
        first.setReady(false);
        second.setReady(false);
        var readiness = new IdentityHashMap<Object, CompletableFuture<Void>>();
        readiness.put("chunk1", first.completion());
        readiness.put("chunk2", second.completion());
        var recorder = new Recorder();
        var channel = new EmbeddedChannel(recorder, new AsyncPacketSendGate(message ->
            readiness.getOrDefault(message, CompletableFuture.completedFuture(null))));
        try {
            var p1 = channel.write("chunk1");
            var p2 = channel.write("chunk2");
            var p3 = channel.write("blockUpdate");
            channel.flush();
            check(recorder.writes.isEmpty() && recorder.flushes == 0, "Unready packet or flush escaped");
            second.setReady(true);
            channel.runPendingTasks();
            check(recorder.writes.isEmpty(), "Second chunk overtook first chunk");
            var worker = new Thread(() -> first.setReady(true), "test-obfuscation-worker");
            worker.start(); worker.join();
            channel.runPendingTasks();
            check(recorder.writes.equals(java.util.List.of("chunk1", "chunk2", "blockUpdate")), "Packet ordering changed");
            check(recorder.flushes == 1 && p1.isSuccess() && p2.isSuccess() && p3.isSuccess(), "Flush or write promises were lost");
        } finally { channel.finishAndReleaseAll(); }
    }

    private static void failedObfuscation() {
        var state = new AsyncPacketState();
        state.setReady(false);
        var recorder = new Recorder();
        var channel = new EmbeddedChannel(recorder, new AsyncPacketSendGate(message -> state.completion()));
        var buffer = Unpooled.buffer().writeByte(1);
        var promise = channel.writeAndFlush(buffer);
        state.fail(new IllegalStateException("intentional obfuscation failure"));
        try {
            channel.runPendingTasks();
            check(!channel.isOpen() && recorder.writes.isEmpty(), "Failed packet was sent");
            check(promise.isDone() && !promise.isSuccess() && buffer.refCnt() == 0, "Failed write leaked");
            try { channel.checkException(); } catch (IllegalStateException expected) {
                check(expected.getMessage().equals("intentional obfuscation failure"), "Unexpected error");
            }
        } finally { channel.finishAndReleaseAll(); }
    }

    private static void disconnectWhilePending() {
        var state = new AsyncPacketState();
        state.setReady(false);
        var channel = new EmbeddedChannel(new AsyncPacketSendGate(message -> state.completion()));
        var buffer = Unpooled.buffer().writeByte(1);
        var promise = channel.writeAndFlush(buffer);
        channel.close();
        state.setReady(true);
        channel.runPendingTasks();
        check(buffer.refCnt() == 0 && promise.isDone() && !promise.isSuccess(), "Disconnect retained pending writes");
        channel.finishAndReleaseAll();
    }

    private static void sharedPacket() {
        var state = new AsyncPacketState();
        state.setReady(false);
        var first = new EmbeddedChannel(new AsyncPacketSendGate(message -> state.completion()));
        var second = new EmbeddedChannel(new AsyncPacketSendGate(message -> state.completion()));
        try {
            first.writeAndFlush("shared");
            second.writeAndFlush("shared");
            state.setReady(true);
            first.runPendingTasks(); second.runPendingTasks();
            check("shared".equals(first.readOutbound()) && "shared".equals(second.readOutbound()), "Shared packet did not resume every connection");
        } finally { first.finishAndReleaseAll(); second.finishAndReleaseAll(); }
    }

    private static final class TestPacket implements net.minecraft.network.protocol.Packet<net.minecraft.network.PacketListener>,
            gr1mly4memes.felt.bridge.core.network.ChunkPacketReadyBridge {
        final AsyncPacketState state = new AsyncPacketState();
        @Override public net.minecraft.network.protocol.PacketType<TestPacket> type() { return null; }
        @Override public void handle(net.minecraft.network.PacketListener listener) {}
        @Override public boolean isReady() { return state.isReady(); }
        @Override public void setReady(boolean ready) { state.setReady(ready); }
        @Override public CompletableFuture<Void> bridge$completion() { return state.completion(); }
        @Override public void bridge$fail(Throwable failure) { state.fail(failure); }
    }

    private static void bundledPacket() {
        var packet = new TestPacket();
        packet.setReady(false);
        var bundle = new net.minecraft.network.protocol.BundlePacket<net.minecraft.network.PacketListener>(java.util.List.of(packet)) {
            @Override public void handle(net.minecraft.network.PacketListener listener) {}
            @Override public net.minecraft.network.protocol.PacketType<? extends net.minecraft.network.protocol.BundlePacket<net.minecraft.network.PacketListener>> type() { return null; }
        };
        var channel = new EmbeddedChannel(new AsyncPacketSendGate());
        try {
            channel.writeAndFlush(bundle);
            channel.writeAndFlush("afterBundle");
            check(channel.readOutbound() == null, "Bundle containing an unready chunk escaped");
            packet.setReady(true);
            channel.runPendingTasks();
            check(channel.readOutbound() == bundle && "afterBundle".equals(channel.readOutbound()), "Bundle order changed");
        } finally { channel.finishAndReleaseAll(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
