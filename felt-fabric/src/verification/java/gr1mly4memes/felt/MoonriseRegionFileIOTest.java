package gr1mly4memes.felt;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.executor.queue.PrioritisedTaskQueue;
import ca.spottedleaf.concurrentutil.executor.thread.BalancedPrioritisedThreadPool;
import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.io.ChunkSystemRegionFileIO;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionDataController;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionFileType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;

/** Runs the actual native task state machine with controlled storage and real NBT compression. */
public final class MoonriseRegionFileIOTest {
    public static final class Controller extends RegionDataController {
        final Map<Long, byte[]> disk = new ConcurrentHashMap<>();
        final AtomicInteger reads = new AtomicInteger(), decompresses = new AtomicInteger();
        final AtomicInteger compresses = new AtomicInteger(), writes = new AtomicInteger();
        volatile boolean failRead, failDecompress, failCompress, failWrite, retryRead, synchronousRead;
        volatile boolean requireWorkers;

        Controller(PrioritisedExecutor io, PrioritisedExecutor compression) {
            this(RegionFileType.CHUNK_DATA, io, compression);
        }
        Controller(RegionFileType type, PrioritisedExecutor io, PrioritisedExecutor compression) { super(type, io, compression); }
        @Override public RegionFileStorage getCache() { throw new UnsupportedOperationException("Test controller has no region cache"); }
        @Override public String getDebugName() { return "verification storage"; }

        private void stage(String name) {
            if (requireWorkers) check(Thread.currentThread().getName().startsWith(name), "Stage ran on the wrong executor");
        }
        @Override public WriteData startWrite(int x, int z, CompoundTag data) throws IOException {
            stage("compression-"); compresses.incrementAndGet();
            if (failCompress) throw new IOException("Injected compression failure");
            if (data == null) return new WriteData(null, WriteData.WriteResult.DELETE, null, ignored -> disk.remove(key(x, z)));
            var bytes = new ByteArrayOutputStream();
            try (var output = new DataOutputStream(new GZIPOutputStream(bytes))) { NbtIo.write(data, output); }
            byte[] compressed = bytes.toByteArray();
            return new WriteData(data, WriteData.WriteResult.WRITE, null, ignored -> disk.put(key(x, z), compressed));
        }
        @Override public void finishWrite(int x, int z, WriteData data) throws IOException {
            stage("io-"); writes.incrementAndGet();
            if (failWrite) throw new IOException("Injected disk write failure");
            data.write().run(null);
        }
        @Override public ReadData readData(int x, int z) throws IOException {
            stage("io-"); reads.incrementAndGet();
            if (failRead) throw new IOException("Injected disk read failure");
            byte[] data = disk.get(key(x, z));
            if (data == null) return new ReadData(ReadData.ReadResult.NO_DATA, null, null, 0);
            if (synchronousRead) {
                try (var input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))) {
                    return new ReadData(ReadData.ReadResult.SYNC_READ, null, NbtIo.read(input), 0);
                }
            }
            return new ReadData(ReadData.ReadResult.HAS_DATA,
                new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data))), null, 0);
        }
        @Override public CompoundTag finishRead(int x, int z, ReadData data) throws IOException {
            stage("compression-"); decompresses.incrementAndGet();
            try (var input = data.input()) {
                if (failDecompress) throw new IOException("Injected decompression failure");
                if (retryRead) { retryRead = false; return null; }
                return NbtIo.read(input);
            }
        }
    }

    /** Uses covariant getters like the full external Moonrise level contract. */
    public static final class ExternalControllers {
        final Controller chunk = controller(RegionFileType.CHUNK_DATA);
        final Controller poi = controller(RegionFileType.POI_DATA);
        final Controller entity = controller(RegionFileType.ENTITY_DATA);
        public Controller moonrise$getChunkDataController() { return chunk; }
        public Controller moonrise$getPoiChunkDataController() { return poi; }
        public Controller moonrise$getEntityChunkDataController() { return entity; }
        private static Controller controller(RegionFileType type) {
            return new Controller(type, new PrioritisedTaskQueue(), new PrioritisedTaskQueue());
        }
    }

    private static final class Harness {
        final PrioritisedTaskQueue io = new PrioritisedTaskQueue();
        final PrioritisedTaskQueue compression = new PrioritisedTaskQueue();
        final Controller controller = new Controller(io, compression);
        void drain() {
            for (int limit = 0; limit < 1000; limit++) {
                boolean ran = io.executeTask();
                ran |= compression.executeTask();
                if (!ran) {
                    check(!controller.hasTasks(), "I/O task counter leaked");
                    check(controller.getTotalWorkingTasks() == 0, "Working task count did not reach zero");
                    MoonriseRegionFileIO.flush(controller);
                    return;
                }
            }
            throw new AssertionError("I/O tasks failed to drain");
        }
        void save(int x, int z, int value) { MoonriseRegionFileIO.scheduleSave(controller, x, z, tag(value), Priority.NORMAL); }
        CompletableFuture<CompoundTag> load(int x, int z) { return MoonriseRegionFileIOTest.load(controller, x, z); }
    }

    public static void main(String[] args) throws Exception {
        controllerLookup();
        saveLoadAndCopy();
        cancellation();
        readWriteOrdering();
        deferredWritesAndPriorities();
        failureRecovery();
        concurrentWorkers();
        System.out.println("Moonrise region I/O: compression, ordering, cancellation, priorities, failure retention and concurrent workers passed");
    }

    private static void controllerLookup() {
        var external = new ExternalControllers();
        var nativeControllers = new ChunkSystemRegionFileIO() {
            @Override public RegionDataController moonrise$getChunkDataController() { return external.chunk; }
            @Override public RegionDataController moonrise$getPoiChunkDataController() { return external.poi; }
            @Override public RegionDataController moonrise$getEntityChunkDataController() { return external.entity; }
        };
        for (int i = 0; i < 2; i++) {
            for (var type : RegionFileType.values()) {
                var controller = ChunkSystemRegionFileIO.controllerFor(external, type);
                check(controller.type == type, "External controller lookup selected the wrong data type");
                check(ChunkSystemRegionFileIO.controllerFor(nativeControllers, type) == controller,
                    "Native controller lookup selected a different storage instance");
            }
        }
        try {
            ChunkSystemRegionFileIO.controllerFor(new Object(), RegionFileType.CHUNK_DATA);
            throw new AssertionError("Uninstalled native storage was accepted");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("not installed"), "Missing storage diagnostic was unclear");
        }
    }

    private static void saveLoadAndCopy() {
        Harness h = new Harness();
        h.save(-65, 64, 12);
        check(h.controller.disk.isEmpty() && h.controller.hasTasks(), "Save performed I/O inline");
        check(h.compression.executeTask(), "Save compression was not scheduled");
        check(h.controller.disk.isEmpty(), "Compression stage wrote to storage");
        h.drain();
        var first = h.load(-65, 64); var second = h.load(-65, 64);
        check(!first.isDone(), "Read completed inline without pending data");
        h.drain();
        check(value(first.join()) == 12 && value(second.join()) == 12, "Read lost the saved NBT");
        first.join().putInt("value", 99);
        check(value(second.join()) == 12, "Readers shared mutable NBT");
        check(h.controller.reads.get() == 1 && h.controller.decompresses.get() == 1, "Reads were not coalesced");
        h.controller.retryRead = true;
        var retry = h.load(-65, 64); h.drain();
        check(value(retry.join()) == 12 && h.controller.reads.get() == 3, "Header-change retry did not reread");
        int decompressions = h.controller.decompresses.get();
        h.controller.synchronousRead = true;
        var synchronous = h.load(-65, 64); h.drain();
        check(value(synchronous.join()) == 12 && h.controller.decompresses.get() == decompressions,
            "Already decoded data entered the decompression stage");
        MoonriseRegionFileIO.scheduleSave(h.controller, -65, 64, (CompoundTag) null, Priority.NORMAL);
        h.drain(); var missing = h.load(-65, 64); h.drain();
        check(missing.join() == null, "Deletion left chunk data behind");
    }

    private static void cancellation() {
        Harness h = new Harness();
        AtomicInteger callbacks = new AtomicInteger();
        var only = MoonriseRegionFileIO.loadDataAsync(h.controller, 0, 0,
            (data, error) -> callbacks.incrementAndGet(), false, Priority.NORMAL);
        check(only.cancel(), "First reader could not cancel its callback");
        check(!only.cancel(), "Repeated cancellation succeeded");
        h.drain();
        check(callbacks.get() == 0 && h.controller.reads.get() == 0, "Cancelled read performed I/O or invoked callback");
        var first = h.load(0, 0);
        var other = MoonriseRegionFileIO.loadDataAsync(h.controller, 0, 0,
            (data, error) -> callbacks.incrementAndGet(), false, Priority.NORMAL);
        check(other.cancel(), "Joined reader could not cancel");
        h.drain();
        check(first.isDone() && first.join() == null && callbacks.get() == 0, "Cancellation affected another reader");
        h.save(0, 0, 1);
        var pendingWriteReader = MoonriseRegionFileIO.loadDataAsync(h.controller, 0, 0,
            (data, error) -> callbacks.incrementAndGet(), false, Priority.NORMAL);
        check(!pendingWriteReader.cancel() && callbacks.get() == 1, "Completed read had a cancellable callback");
        h.drain();
        var duringRead = MoonriseRegionFileIO.loadDataAsync(h.controller, 0, 0,
            (data, error) -> callbacks.incrementAndGet(), false, Priority.NORMAL);
        check(h.io.executeTask(), "Disk read was not queued");
        check(duringRead.cancel(), "In-progress reader could not cancel its callback");
        h.drain();
        check(callbacks.get() == 1, "In-progress cancellation invoked the callback");
    }

    private static void readWriteOrdering() {
        Harness h = new Harness();
        h.save(0, 0, 1); h.drain();
        var oldRead = h.load(0, 0);
        h.save(0, 0, 2);
        var newRead = h.load(0, 0);
        check(newRead.isDone() && value(newRead.join()) == 2, "Reader ignored the pending save");
        newRead.join().putInt("value", 99);
        h.drain();
        check(value(oldRead.join()) == 1, "Earlier read observed a later write");
        var saved = h.load(0, 0); h.drain();
        check(value(saved.join()) == 2, "Pending write was lost after read");
    }

    private static void deferredWritesAndPriorities() {
        Harness h = new Harness();
        var first = new CompletableFuture<CompoundTag>();
        var second = new CompletableFuture<CompoundTag>();
        var cancelledCallbacks = new AtomicInteger();
        var serializer = h.compression.createTask(() -> {}, Priority.LOW);
        MoonriseRegionFileIO.scheduleSave(h.controller, 1, 1, first::whenComplete, serializer, Priority.LOW);
        var cancelled = MoonriseRegionFileIO.loadDataAsync(h.controller, 1, 1,
            (data, error) -> cancelledCallbacks.incrementAndGet(), false, Priority.NORMAL);
        check(cancelled.cancel(), "Pending serialization reader could not cancel");
        var readFirst = h.load(1, 1);
        MoonriseRegionFileIO.raisePriority(h.controller, 1, 1, Priority.HIGHEST);
        check(serializer.getPriority() == Priority.HIGHEST, "Serialization priority did not follow I/O");
        MoonriseRegionFileIO.lowerPriority(h.controller, 1, 1, Priority.LOW);
        check(MoonriseRegionFileIO.getPriority(h.controller, 1, 1) == Priority.LOW, "Priority lowering failed");
        MoonriseRegionFileIO.setPriority(h.controller, 1, 1, Priority.NORMAL);
        MoonriseRegionFileIO.scheduleSave(h.controller, 1, 1, second::whenComplete, null, Priority.NORMAL);
        var readSecond = h.load(1, 1);
        second.complete(tag(2)); first.complete(tag(1));
        h.drain();
        check(cancelledCallbacks.get() == 0, "Cancelled deferred reader ran");
        check(value(readFirst.join()) == 1 && value(readSecond.join()) == 2, "Readers lost serialization ordering");
        var latest = h.load(1, 1); h.drain();
        check(value(latest.join()) == 2, "Stale serialization overwrote the latest write");
    }

    private static void failureRecovery() {
        Harness h = new Harness();
        h.controller.failRead = true;
        var badRead = h.load(0, 0); h.drain();
        check(badRead.isCompletedExceptionally(), "Read failure was swallowed");
        h.controller.failRead = false;
        h.save(0, 0, 1); h.drain();
        h.controller.failDecompress = true;
        var badNbt = h.load(0, 0); h.drain();
        check(badNbt.isCompletedExceptionally(), "Decompression failure was swallowed");
        h.controller.failDecompress = false;
        h.controller.failWrite = true;
        h.save(0, 0, 2); h.drain();
        var retained = h.load(0, 0);
        check(retained.isDone() && value(retained.join()) == 2, "Failed disk write lost pending data");
        h.controller.failWrite = false;
        h.save(0, 0, 3); h.drain();
        var recovered = h.load(0, 0); h.drain();
        check(value(recovered.join()) == 3, "New write did not replace failed write");
        h.controller.failCompress = true;
        h.save(0, 0, 4); h.drain();
        check(value(h.load(0, 0).join()) == 4, "Failed compression lost the pending data");
        h.controller.failCompress = false;
        h.save(0, 0, 5); h.drain();
        var finalRead = h.load(0, 0); h.drain();
        check(value(finalRead.join()) == 5, "Compression recovery failed");
        var serialization = new CompletableFuture<CompoundTag>();
        MoonriseRegionFileIO.scheduleSave(h.controller, 0, 0, serialization::whenComplete, null, Priority.NORMAL);
        var waiting = h.load(0, 0);
        serialization.completeExceptionally(new IOException("Injected serialization failure"));
        h.drain();
        check(waiting.isCompletedExceptionally(), "Serialization failure was not delivered to waiting readers");
        var unchanged = h.load(0, 0); h.drain();
        check(value(unchanged.join()) == 5, "Failed serialization changed saved data");
    }

    private static void concurrentWorkers() throws Exception {
        var ioIds = new AtomicInteger(); var compressionIds = new AtomicInteger();
        var ioPool = new BalancedPrioritisedThreadPool(1_000_000L,
            run -> new Thread(run, "io-" + ioIds.incrementAndGet()));
        var compressionPool = new BalancedPrioritisedThreadPool(1_000_000L,
            run -> new Thread(run, "compression-" + compressionIds.incrementAndGet()));
        var io = ioPool.createOrderedStreamGroup().createExecutor();
        var compression = compressionPool.createOrderedStreamGroup().createExecutor();
        Controller controller = new Controller(io, compression);
        controller.requireWorkers = true;
        ioPool.adjustThreadCount(3); compressionPool.adjustThreadCount(3);
        try {
            List<CompletableFuture<CompoundTag>> pending = new ArrayList<>();
            for (int i = 0; i < 120; i++) {
                int x = i % 4 == 0 ? -65 : i;
                int z = (i % 5) * 1024;
                MoonriseRegionFileIO.scheduleSave(controller, x, z, tag(i), Priority.NORMAL);
                pending.add(load(controller, x, z));
            }
            for (int i = 0; i < pending.size(); i++) check(value(pending.get(i).get(15, TimeUnit.SECONDS)) == i,
                "Concurrent read-after-save observed another value");
            awaitIdle(controller);
            List<CompletableFuture<CompoundTag>> diskReads = new ArrayList<>();
            Map<Long, Integer> expected = new java.util.LinkedHashMap<>();
            for (int i = 0; i < 120; i++) expected.put(key(i % 4 == 0 ? -65 : i, (i % 5) * 1024), i);
            for (long key : expected.keySet()) diskReads.add(load(controller, (int) key, (int) (key >>> 32)));
            int index = 0;
            for (int value : expected.values()) check(value(diskReads.get(index++).get(15, TimeUnit.SECONDS)) == value,
                "Concurrent disk read lost latest save");
            awaitIdle(controller);
            check(controller.reads.get() > 0 && controller.decompresses.get() > 0, "Worker I/O stages were skipped");
        } finally {
            ioPool.halt(true); compressionPool.halt(true);
            check(ioPool.joinInterruptable(10_000), "I/O pool did not stop");
            check(compressionPool.joinInterruptable(10_000), "Compression pool did not stop");
        }
    }

    private static void awaitIdle(Controller controller) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (controller.hasTasks() && System.nanoTime() < deadline) Thread.sleep(1);
        check(!controller.hasTasks(), "Worker I/O tasks did not finish");
        MoonriseRegionFileIO.flush(controller);
    }
    private static CompletableFuture<CompoundTag> load(Controller controller, int x, int z) {
        var future = new CompletableFuture<CompoundTag>();
        MoonriseRegionFileIO.loadDataAsync(controller, x, z, (data, error) -> {
            if (error == null) future.complete(data); else future.completeExceptionally(error);
        }, false, Priority.NORMAL);
        return future;
    }
    private static CompoundTag tag(int value) { var tag = new CompoundTag(); tag.putInt("value", value); return tag; }
    private static int value(CompoundTag tag) { return tag.getIntOr("value", Integer.MIN_VALUE); }
    private static long key(int x, int z) { return (x & 0xffffffffL) | ((long) z << 32); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
