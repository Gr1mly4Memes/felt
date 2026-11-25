package gr1mly4memes.felt;

import ca.spottedleaf.concurrentutil.lock.ReentrantAreaLock;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ThreadedTicketLevelPropagator;
import it.unimi.dsi.fastutil.longs.Long2ByteLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Compare the ported propagation engine with an independent distance oracle. */
public final class MoonriseTicketPropagationTest {
    private static final class Propagator extends ThreadedTicketLevelPropagator<Long, Long> {
        final int schedulingRadius;
        final ReentrantAreaLock ticketLock = new ReentrantAreaLock(SECTION_SHIFT);
        final ReentrantAreaLock schedulingLock = new ReentrantAreaLock(SECTION_SHIFT);
        final Map<Long, Integer> sources = new ConcurrentHashMap<>();
        final Map<Long, Integer> levels = new ConcurrentHashMap<>();

        Propagator() { this(16); }
        Propagator(int schedulingRadius) { super(schedulingRadius); this.schedulingRadius = schedulingRadius; }

        @Override protected void processLevelUpdates(Long2ByteLinkedOpenHashMap updates) {
            for (var entry : updates.long2ByteEntrySet()) {
                long key = entry.getLongKey();
                check(ticketLock.isHeldByCurrentThread(x(key), z(key)), "Level callback lacks ticket lock");
                if (entry.getByteValue() == 0) levels.remove(key);
                else levels.put(key, (int) entry.getByteValue());
            }
        }

        @Override protected void processSchedulingUpdates(Long2ByteLinkedOpenHashMap updates,
                                                           List<Long> tasks, List<Long> changedHolders) {
            for (var entry : updates.long2ByteEntrySet()) {
                long key = entry.getLongKey();
                check(schedulingLock.isHeldByCurrentThread(x(key), z(key), schedulingRadius),
                    "Scheduling callback lacks radius lock");
                check(levels.getOrDefault(key, 0) == entry.getByteValue(), "Scheduling preceded level update");
                tasks.add(key);
                changedHolders.add(key);
            }
        }

        void source(int x, int z, int level) {
            int minX = (x >> SECTION_SHIFT) << SECTION_SHIFT;
            int minZ = (z >> SECTION_SHIFT) << SECTION_SHIFT;
            var node = ticketLock.lock(minX, minZ, minX + SECTION_SIZE - 1, minZ + SECTION_SIZE - 1);
            try {
                if (level == 0) {
                    removeSource(x, z);
                    sources.remove(key(x, z));
                } else {
                    setSource(x, z, level);
                    sources.put(key(x, z), level);
                }
            } finally {
                ticketLock.unlock(node);
            }
        }

        boolean drain() {
            var tasks = new ArrayList<Long>();
            var holders = new ArrayList<Long>();
            boolean changed = performUpdates(ticketLock, schedulingLock, tasks, holders);
            check(tasks.equals(holders), "Callback output lists were not preserved");
            return changed;
        }

        void verify() {
            drain();
            check(!hasPendingUpdates(), "Update queue failed to drain");
            Map<Long, Integer> expected = new HashMap<>();
            // A ticket contributes level - Chebyshev distance, clamped at zero.
            for (var source : sources.entrySet()) {
                int level = source.getValue();
                int sx = x(source.getKey()), sz = z(source.getKey());
                for (int dz = 1 - level; dz < level; dz++) {
                    for (int dx = 1 - level; dx < level; dx++) {
                        int value = level - Math.max(Math.abs(dx), Math.abs(dz));
                        expected.merge(key(sx + dx, sz + dz), value, Math::max);
                    }
                }
            }
            if (!expected.equals(levels)) {
                var differences = expected.entrySet().stream()
                    .filter(entry -> !entry.getValue().equals(levels.get(entry.getKey())))
                    .limit(8).map(entry -> "(" + x(entry.getKey()) + "," + z(entry.getKey()) + "): expected "
                        + entry.getValue() + ", got " + levels.get(entry.getKey())).toList();
                throw new AssertionError("Propagation differs from distance oracle: " + differences + "; sources=" + sources);
            }
            check(!drain(), "Idle queue reported a change");
        }
    }

    public static void main(String[] args) throws Exception {
        boundariesAndRemoval();
        overlappingSourceDecrease();
        coalescingAndStolenUpdate();
        randomUpdates();
        concurrentUpdates(false, 16);
        concurrentUpdates(true, 0);
        concurrentUpdates(true, 128);
        System.out.println("Moonrise ticket propagation: distance oracle, section boundaries, removal, coalescing, lock coverage and concurrent workers passed");
    }

    private static void boundariesAndRemoval() {
        Propagator p = new Propagator();
        int[][] positions = {{-65, -64}, {-1, 0}, {63, 64}, {64, -1}, {0, 0}};
        for (int[] pos : positions) { p.source(pos[0], pos[1], 62); p.verify(); }
        p.source(-1, 0, 2); p.verify();
        for (int[] pos : positions) { p.source(pos[0], pos[1], 0); p.verify(); }
        check(p.levels.isEmpty(), "Removed sources left loaded chunks");
        p.source(-65, -64, 1); p.verify();
        check(p.levels.size() == 1, "Minimum source propagated beyond itself");
        for (int invalid : new int[] {-1, 0, 63}) {
            try { p.setSource(0, 0, invalid); throw new AssertionError("Invalid source accepted"); }
            catch (IllegalArgumentException expected) { /* Source bounds are checked before mutation. */ }
        }
    }

    private static void coalescingAndStolenUpdate() {
        Propagator p = new Propagator();
        p.source(63, -1, 12); p.source(63, -1, 30); p.source(63, -1, 0); p.verify();
        p.source(63, -1, 12); p.verify();
        p.source(63, -1, 20); p.source(63, -1, 12); p.verify();
        p.source(63, -1, 0); p.source(63, -1, 12); p.verify();
        p.source(63, -1, 31);
        // A synchronous chunk request can steal a section update from the worker queue.
        var node = p.ticketLock.lock(-64, -128, 127, 63);
        try {
            check(p.performUpdate(0, -1, p.schedulingLock, new ArrayList<>(), new ArrayList<>()),
                "Section update was not available to steal");
        } finally {
            p.ticketLock.unlock(node);
        }
        p.verify();
        p.source(63, -1, 0); p.verify();
        p.source(63, -1, 31); p.verify();
    }

    private static void overlappingSourceDecrease() {
        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                Propagator p = new Propagator();
                p.source(-64, 127, 14); p.source(-64 + dx, 127 + dz, 13); p.verify();
                p.source(-64, 127, 2); p.verify();
                p.source(-64 + dx, 127 + dz, 0); p.verify();
            }
        }
    }

    private static void randomUpdates() {
        Propagator p = new Propagator();
        Random random = new Random(0xF317);
        int[] coordinates = {-129, -65, -64, -1, 0, 1, 63, 64, 127, 128};
        for (int batch = 0; batch < 70; batch++) {
            for (int i = 0; i < 6; i++) {
                int x = coordinates[random.nextInt(coordinates.length)];
                int z = coordinates[random.nextInt(coordinates.length)];
                int level = random.nextBoolean() ? 0 : 1 + random.nextInt(62);
                p.source(x, z, level);
            }
            try { p.verify(); }
            catch (AssertionError failure) { throw new AssertionError("Random batch " + batch, failure); }
        }
        for (long key : List.copyOf(p.sources.keySet())) p.source(x(key), z(key), 0);
        p.verify();
    }

    private static void concurrentUpdates(boolean separateRegions, int radius) throws Exception {
        Propagator p = new Propagator(radius);
        var executor = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<java.util.concurrent.Future<?>>();
        try {
            for (int worker = 0; worker < 4; worker++) {
                int id = worker;
                futures.add(executor.submit(() -> {
                    try { start.await(); }
                    catch (InterruptedException failure) { throw new AssertionError(failure); }
                    for (int round = 0; round < 40; round++) {
                        int x = (round % 4) * 64 - 65 + id;
                        int z = separateRegions ? (id / 2) * 1024 - 65 : id * 64 - 65;
                        p.source(x, z, round % 3 == 0 ? 0 : 8 + round % 24);
                        p.drain();
                    }
                }));
            }
            start.countDown();
            for (var future : futures) future.get(45, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            check(executor.awaitTermination(10, TimeUnit.SECONDS), "Propagation workers did not terminate");
        }
        p.verify();
        for (long key : List.copyOf(p.sources.keySet())) p.source(x(key), z(key), 0);
        p.verify();
    }

    private static long key(int x, int z) { return (x & 0xffffffffL) | ((long) z << 32); }
    private static int x(long key) { return (int) key; }
    private static int z(long key) { return (int) (key >>> 32); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
