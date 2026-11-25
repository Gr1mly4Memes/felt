package gr1mly4memes.felt;

import ca.spottedleaf.concurrentutil.executor.queue.PrioritisedTaskQueue;
import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.io.ChunkSystemRegionFileStorage;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionDataController.ReadData;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionFileType;
import ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.MoonriseRegionFileDataController;
import ca.spottedleaf.moonrise.patches.chunk_system.storage.ChunkSystemRegionFile;
import ca.spottedleaf.moonrise.patches.chunk_system.storage.MoonriseRegionFileAccess;
import ca.spottedleaf.moonrise.patches.chunk_system.storage.MoonriseRegionFileStorageAccess;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

/** File-level verification is also run inside Fabric to exercise the actual mixin bindings. */
public final class MoonriseRegionFilesTest {
    public static void main(String[] args) throws Exception {
        Path base = Path.of("build").toAbsolutePath().normalize();
        Files.createDirectories(base);
        Path root = Files.createTempDirectory(base, "moonrise-region-files-").toRealPath();
        check(root.startsWith(base.toRealPath()), "Verification directory escaped build directory");
        try {
            pipelineRoundTrip(root.resolve("roundtrip"));
            detachedCommitAfterEviction(root.resolve("eviction"));
            missingRegions(root.resolve("missing"));
            externalChunks(root.resolve("external"));
            swappedHeaderRecovery(root.resolve("swapped"));
            invalidHeaderRecovery(root.resolve("invalid"));
            entityPosition(root.resolve("entities"));
            concurrentEviction(root.resolve("concurrent"));
            System.out.println("Moonrise region files: durable pipeline writes, eviction, missing files, external chunks and header recovery passed"
                + (Boolean.getBoolean("felt.verify.storageMixins") ? " (Fabric mixins active)" : ""));
        } finally {
            // Remove only the unique directory created above, after all region channels have closed.
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    check(path.toAbsolutePath().normalize().startsWith(root), "Cleanup escaped verification directory");
                    Files.delete(path);
                }
            }
        }
    }

    private static RegionFileStorage storage(Path folder, String type) {
        return new RegionFileStorage(new RegionStorageInfo("verification",
            ResourceKey.create(Registries.DIMENSION, Identifier.parse("minecraft:overworld")), type), folder, false);
    }

    private static CompoundTag chunk(int x, int z, long time, int value) {
        var data = new CompoundTag();
        data.putInt("DataVersion", 2842); // Use the modern root layout recognized by RegionFileRecovery.
        data.putInt("xPos", x); data.putInt("zPos", z); data.putLong("LastUpdate", time); data.putInt("value", value);
        return data;
    }

    private static void pipelineRoundTrip(Path folder) throws Exception {
        var io = new PrioritisedTaskQueue(); var compression = new PrioritisedTaskQueue();
        CompoundTag expected = chunk(-65, 64, 100, 42);
        try (var cache = storage(folder, "chunk")) {
            if (Boolean.getBoolean("felt.verify.storageMixins")) {
                check((Object) cache instanceof ChunkSystemRegionFileStorage, "Storage mixin did not apply");
            }
            var controller = new MoonriseRegionFileDataController(cache, RegionFileType.CHUNK_DATA, io, compression);
            MoonriseRegionFileIO.scheduleSave(controller, -65, 64, expected, Priority.NORMAL);
            check(compression.executeTask(), "Write was not prepared asynchronously");
            check(cache.read(new ChunkPos(-65, 64)) == null, "Preparation committed data before I/O");
            drain(controller, io, compression);
            var result = load(controller, -65, 64); drain(controller, io, compression);
            check(expected.equals(result.join()), "Native I/O pipeline did not round-trip NBT");
            cache.flush();
            RegionFile region = cache.regionCache.values().iterator().next().orElseThrow();
            if (Boolean.getBoolean("felt.verify.storageMixins")) {
                check(region instanceof ChunkSystemRegionFile && region instanceof MoonriseRegionFileAccess.Owner,
                    "Region file mixin did not apply");
                check(((MoonriseRegionFileAccess.Owner) region).moonrise$getRegionFileAccess().moonrise$getRecalculateCount()
                    == ((ChunkSystemRegionFile) region).moonrise$getRecalculateCount(), "Recovery generation was not shared");
            }
        }
        try (var cache = storage(folder, "chunk")) {
            check(expected.equals(cache.read(new ChunkPos(-65, 64))), "Saved data was not readable after reopening");
        }
    }

    private static void detachedCommitAfterEviction(Path folder) throws Exception {
        try (var cache = storage(folder, "chunk")) {
            var access = new MoonriseRegionFileStorageAccess(cache, () -> 1);
            var a = chunk(0, 0, 10, 1); var b = chunk(-65, 64, 10, 2);
            var preparedA = access.moonrise$startWrite(0, 0, a);
            RegionFile first = access.moonrise$getRegionFileIfLoaded(0, 0);
            var preparedB = access.moonrise$startWrite(-65, 64, b);
            check(access.moonrise$getRegionFileIfLoaded(0, 0) == null, "Prepared write pinned an evicted file");
            check(!first.file.isOpen(), "Evicted region channel leaked");
            access.moonrise$finishWrite(0, 0, preparedA);
            access.moonrise$finishWrite(-65, 64, preparedB);
            check(a.equals(read(access, 0, 0)) && b.equals(read(access, -65, 64)), "Detached commit used a closed region file");
            RegionFile loaded = access.moonrise$getRegionFileIfLoaded(-65, 64);
            check(access.moonrise$getRegionFileIfExists(4096, 4096) == null, "Missing region was created by lookup");
            check(access.moonrise$getRegionFileIfLoaded(-65, 64) == loaded, "Missing lookup evicted a live region");
            check(cache.regionCache.size() == 1, "Region cache exceeded its limit");
        }
    }

    private static void missingRegions(Path folder) throws Exception {
        try (var cache = storage(folder, "poi")) {
            var access = new MoonriseRegionFileStorageAccess(cache);
            check(access.moonrise$readData(-1, -1).result() == ReadData.ReadResult.NO_DATA, "Missing region returned data");
            check(access.moonrise$doesRegionFileNotExistNoIO(-1, -1), "Read miss was not cached");
            var deletion = access.moonrise$startWrite(-1, -1, null);
            access.moonrise$finishWrite(-1, -1, deletion);
            check(Files.notExists(folder), "Deleting a missing chunk created region storage");
            var data = new CompoundTag(); data.putInt("value", 7);
            var write = access.moonrise$startWrite(-1, -1, data); access.moonrise$finishWrite(-1, -1, write);
            check(!access.moonrise$doesRegionFileNotExistNoIO(-1, -1), "Created region remained in missing cache");
            check(data.equals(read(access, -1, -1)), "POI data was treated as chunk coordinate data");
            access.moonrise$finishWrite(-1, -1, deletion);
            check(read(access, -1, -1) == null, "Chunk deletion retained saved data");
        }
    }

    private static void externalChunks(Path folder) throws Exception {
        try (var cache = storage(folder, "chunk")) {
            var access = new MoonriseRegionFileStorageAccess(cache);
            var data = chunk(-1, -1, 100, 9);
            byte[] random = new byte[1_300_000]; new Random(413).nextBytes(random); data.putByteArray("noise", random);
            var write = access.moonrise$startWrite(-1, -1, data); access.moonrise$finishWrite(-1, -1, write);
            check(Files.isRegularFile(folder.resolve("c.-1.-1.mcc")), "Oversized chunk was not stored externally");
            var loaded = access.moonrise$readData(-1, -1);
            check(loaded.result() == ReadData.ReadResult.SYNC_READ && loaded.input() == null,
                "External chunk stream escaped the region I/O stage");
            check(data.equals(loaded.syncRead()), "External chunk NBT changed");
        }
    }

    private static void swappedHeaderRecovery(Path folder) throws Exception {
        var a = chunk(0, 0, 100, 1); var b = chunk(1, 0, 200, 2);
        try (var cache = storage(folder, "chunk")) { cache.write(new ChunkPos(0, 0), a); cache.write(new ChunkPos(1, 0), b); }
        Path path = folder.resolve("r.0.0.mca");
        try (var file = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer offsets = ByteBuffer.allocate(8); readFully(file, offsets, 0); offsets.flip();
            int first = offsets.getInt(), second = offsets.getInt();
            offsets.clear().putInt(second).putInt(first).flip(); writeFully(file, offsets, 0);
        }
        try (var cache = storage(folder, "chunk")) {
            var access = new MoonriseRegionFileStorageAccess(cache);
            check(a.equals(read(access, 0, 0)) && b.equals(read(access, 1, 0)), "Swapped header returned the wrong chunks");
        }
        try (var files = Files.list(folder)) { check(files.anyMatch(p -> p.toString().endsWith(".backup")), "Recovery omitted its backup"); }
        try (var cache = storage(folder, "chunk")) { check(a.equals(cache.read(new ChunkPos(0, 0))), "Recovered header was not persisted"); }
    }

    private static void invalidHeaderRecovery(Path folder) throws Exception {
        var data = chunk(0, 0, 200, 4);
        try (var cache = storage(folder, "chunk")) {
            cache.write(new ChunkPos(0, 0), data);
            cache.write(new ChunkPos(0, 0), chunk(0, 0, 100, 5)); // A freed sector still holds the newer saved version.
        }
        Path path = folder.resolve("r.0.0.mca");
        try (var file = FileChannel.open(path, StandardOpenOption.WRITE)) {
            writeFully(file, ByteBuffer.allocate(4).putInt(257).flip(), 0); // Invalid: points into header sector 1.
        }
        try (var region = new RegionFile(storageInfo("chunk"), path, folder, false)) {
            var access = region instanceof MoonriseRegionFileAccess.Owner owner ? owner.moonrise$getRegionFileAccess()
                : new MoonriseRegionFileAccess(region);
            access.recoverHeaderIfNeeded();
            int generation = access.moonrise$getRecalculateCount();
            check(generation > 0, "Constructor repair was not followed by native header recovery");
            try (var input = access.readInput(new ChunkPos(0, 0))) { check(data.equals(NbtIo.read(input)), "Recovered chunk was lost"); }
            check(access.moonrise$tryRecalcHeader(generation - 1) == null, "Stale recovery generation was accepted");
        }
    }

    private static RegionStorageInfo storageInfo(String type) {
        return new RegionStorageInfo("verification", ResourceKey.create(Registries.DIMENSION, Identifier.parse("minecraft:overworld")), type);
    }

    private static void concurrentEviction(Path folder) throws Exception {
        try (var cache = storage(folder, "chunk")) {
            var access = new MoonriseRegionFileStorageAccess(cache, () -> 1);
            var executor = Executors.newFixedThreadPool(4);
            var futures = new ArrayList<Future<?>>();
            try {
                for (int worker = 0; worker < 4; worker++) {
                    int id = worker;
                    futures.add(executor.submit(() -> {
                        try {
                            for (int i = 0; i < 30; i++) {
                                int x = (i - 15) * 32, z = id * 1024;
                                var data = chunk(x, z, 10, i);
                                var prepared = access.moonrise$startWrite(x, z, data);
                                access.moonrise$finishWrite(x, z, prepared);
                                check(data.equals(read(access, x, z)), "Concurrent eviction changed saved data");
                            }
                        } catch (IOException failure) { throw new IllegalStateException(failure); }
                    }));
                }
                for (var future : futures) future.get(30, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
                check(executor.awaitTermination(10, TimeUnit.SECONDS), "Concurrent file workers did not stop");
            }
        }
    }

    private static void entityPosition(Path folder) throws Exception {
        try (var cache = storage(folder, "entities")) {
            var controller = new MoonriseRegionFileDataController(cache, RegionFileType.ENTITY_DATA,
                new PrioritisedTaskQueue(), new PrioritisedTaskQueue());
            var data = new CompoundTag(); data.store("Position", ChunkPos.CODEC, new ChunkPos(5, 5));
            try { controller.startWrite(0, 0, data); throw new AssertionError("Mismatched entity position was accepted"); }
            catch (IllegalArgumentException expected) { /* Paper rejects entity relocation before serialization. */ }
        }
    }

    private static CompoundTag read(MoonriseRegionFileStorageAccess access, int x, int z) throws IOException {
        for (int retry = 0; retry < 3; retry++) {
            var data = access.moonrise$readData(x, z);
            if (data.result() != ReadData.ReadResult.HAS_DATA) return data.syncRead();
            var result = access.moonrise$finishRead(x, z, data);
            if (result != null) return result;
        }
        throw new AssertionError("Region read never completed after recovery");
    }
    private static CompletableFuture<CompoundTag> load(MoonriseRegionFileDataController controller, int x, int z) {
        var future = new CompletableFuture<CompoundTag>();
        MoonriseRegionFileIO.loadDataAsync(controller, x, z, (data, error) -> {
            if (error == null) future.complete(data); else future.completeExceptionally(error);
        }, false, Priority.NORMAL);
        return future;
    }
    private static void drain(MoonriseRegionFileDataController controller, PrioritisedTaskQueue io, PrioritisedTaskQueue compression) {
        for (int i = 0; i < 1000; i++) {
            boolean ran = io.executeTask(); ran |= compression.executeTask();
            if (!ran) { check(!controller.hasTasks(), "Native I/O task leaked"); return; }
        }
        throw new AssertionError("Native I/O queue did not drain");
    }
    private static void readFully(FileChannel file, ByteBuffer data, long position) throws IOException {
        while (data.hasRemaining()) { int read = file.read(data, position); if (read <= 0) throw new IOException("Truncated header"); position += read; }
    }
    private static void writeFully(FileChannel file, ByteBuffer data, long position) throws IOException {
        while (data.hasRemaining()) { int written = file.write(data, position); if (written <= 0) throw new IOException("Write stalled"); position += written; }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
