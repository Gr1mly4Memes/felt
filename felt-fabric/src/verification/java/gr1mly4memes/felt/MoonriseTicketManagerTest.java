package gr1mly4memes.felt;

import ca.spottedleaf.concurrentutil.lock.ReentrantAreaLock;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.MoonriseTicketManager;
import ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicket;
import ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType;
import ca.spottedleaf.moonrise.patches.chunk_system.util.stream.TicketSet;
import ca.spottedleaf.moonrise.common.util.TickThread;
import gr1mly4memes.felt.bridge.core.server.level.TicketBridge;
import gr1mly4memes.felt.bridge.core.server.level.TicketTypeBridge;
import it.unimi.dsi.fastutil.longs.Long2ByteLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;

/** Uses actual Ticket and TicketType mixins inside Fabric, without creating a server world. */
public final class MoonriseTicketManagerTest {
    private static int LEVEL;
    private static final class Manager extends MoonriseTicketManager<Runnable, Long> {
        final Map<Long, Integer> levels = new ConcurrentHashMap<>();
        final ConcurrentLinkedQueue<Long> pendingStatuses = new ConcurrentLinkedQueue<>();
        final AtomicInteger tasksStarted = new AtomicInteger();
        Manager() { super(4, 16, new ReentrantAreaLock(6)); }

        @Override protected void processLevelUpdates(Long2ByteLinkedOpenHashMap updates) {
            for (var entry : updates.long2ByteEntrySet()) {
                long pos = entry.getLongKey();
                check(ticketLockArea.isHeldByCurrentThread(x(pos), z(pos)), "Level callback lacks ticket lock");
                int level = convertBetweenTicketLevels(entry.getByteValue());
                if (level > MAX_TICKET_LEVEL) levels.remove(pos); else levels.put(pos, level);
            }
        }
        @Override protected void processSchedulingUpdates(Long2ByteLinkedOpenHashMap updates, List<Runnable> tasks, List<Long> statuses) {
            for (var entry : updates.long2ByteEntrySet()) {
                long pos = entry.getLongKey();
                check(schedulingLockArea.isHeldByCurrentThread(x(pos), z(pos), 16), "Scheduling callback lacks radius lock");
                statuses.add(pos);
                tasks.add(tasksStarted::incrementAndGet);
            }
        }
        @Override protected void afterTicketUpdates(List<Runnable> tasks, List<Long> statuses) {
            for (long pos : statuses) {
                check(!ticketLockArea.isHeldByCurrentThread(x(pos), z(pos)), "Task dispatch still holds ticket lock");
                pendingStatuses.add(pos);
            }
            for (Runnable task : tasks) task.run();
        }
        @Override protected boolean processPendingFullUpdates() {
            boolean changed = !pendingStatuses.isEmpty(); pendingStatuses.clear(); return changed;
        }
        long count(long counter, long pos) {
            var counts = getTicketCounters(counter); return counts == null ? 0 : counts.getOrDefault(pos, 0);
        }
        void verifyLevels() {
            processTicketUpdates();
            Map<Long, Integer> expected = new HashMap<>();
            for (var entry : getTicketsCopy().long2ObjectEntrySet()) {
                int sx = x(entry.getLongKey()), sz = z(entry.getLongKey());
                int best = entry.getValue().stream().mapToInt(Ticket::getTicketLevel).min().orElse(MAX_TICKET_LEVEL + 1);
                int radius = MAX_TICKET_LEVEL - best;
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int level = best + Math.max(Math.abs(dx), Math.abs(dz));
                        expected.merge(key(sx + dx, sz + dz), level, Math::min);
                    }
                }
            }
            check(expected.equals(levels), "Ticket storage and propagated levels disagree");
        }
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        for (String target : List.of("net.minecraft.server.level.ServerLevel", "net.minecraft.server.level.ServerChunkCache", "net.minecraft.server.level.ChunkMap", "net.minecraft.server.level.ChunkHolder", "net.minecraft.server.level.ServerPlayer", "net.minecraft.world.entity.ai.village.poi.PoiManager", "net.minecraft.world.level.chunk.storage.SerializableChunkData", "net.minecraft.world.level.chunk.storage.SerializableChunkData$SectionData", "net.minecraft.world.level.lighting.LevelLightEngine", "net.minecraft.server.level.ThreadedLevelLightEngine", "net.minecraft.world.ticks.LevelChunkTicks", "net.minecraft.util.SortedArraySet")) Class.forName(target);
        check(ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.getMaxAccessRadius() > 0, "Native status scheduling radius missing");

        LEVEL = MoonriseTicketManager.MAX_TICKET_LEVEL - 8;
        mixinContracts();
        sortedCollection();
        expiryAndRefresh();
        countersAndAtomicOperations();
        pluginTickets();
        randomizedChanges();
        concurrentChanges();
        tickThreadDispatch();
        System.out.println("Moonrise ticket manager: mixins, ordering, expiry, counters, atomic operations and concurrent propagation passed");
    }

    @SuppressWarnings("unchecked")
    private static ChunkSystemTicket<Object> nativeTicket(Ticket ticket) { return (ChunkSystemTicket<Object>) (Object) ticket; }
    @SuppressWarnings("unchecked")
    private static ChunkSystemTicketType<Object> nativeType(TicketType type) { return (ChunkSystemTicketType<Object>) (Object) type; }
    private static TicketType type(long timeout) { return ChunkSystemTicketType.create("verification", Comparator.<Integer>naturalOrder(), timeout); }
    private static Ticket ticket(TicketType type, int level, Object id, long delay) {
        var ticket = new Ticket(type, level, delay); nativeTicket(ticket).moonrise$setIdentifier(id); return ticket;
    }

    private static void mixinContracts() {
        check(ChatFormatting.getByHexValue(0xFF5555) == ChatFormatting.RED, "A compilation-only static method reached runtime");
        TicketType a = type(5), b = type(5);
        check(nativeType(a).moonrise$getId() != nativeType(b).moonrise$getId(), "Ticket type IDs collided");
        Ticket first = ticket(a, LEVEL, 10, 5), second = ticket(a, LEVEL, 20, 5);
        check(nativeTicket(first).compareTo(second) < 0, "Identifier ordering was lost");
        check(nativeTicket(first).compareTo(ticket(b, LEVEL, 10, 5)) != 0, "Different ticket types compared equal");
        check(((TicketBridge) (Object) first).bridge$getKey().equals(10), "Native and Felt identifiers diverged");
        nativeType(a).moonrise$setTimeout(7);
        check(a.timeout() == 7, "Native timeout setter did not update record storage");
        try { nativeTicket(first).moonrise$setIdentifier(null); throw new AssertionError("Missing identifier was accepted"); }
        catch (IllegalStateException expected) { /* Comparator types require identifiers. */ }
        Ticket permanent = ticket(a, LEVEL, 30, Long.MIN_VALUE);
        check(!MoonriseTicketManager.tickTicket(permanent) && nativeTicket(permanent).moonrise$getRemoveDelay() == Long.MIN_VALUE,
            "Permanent ticket countdown changed");
    }

    private static void sortedCollection() {
        TicketType type = type(0);
        var set = new TicketSet(0);
        var expected = new java.util.TreeMap<Integer, Ticket>();
        Random random = new Random(1987);
        for (int i = 0; i < 300; i++) {
            int id = random.nextInt(80);
            var value = ticket(type, LEVEL, id, Long.MIN_VALUE);
            Ticket previous = expected.put(id, value);
            check(set.replace(value) == (previous == null ? value : previous), "Ticket replacement returned the wrong instance");
            check(new ArrayList<>(set).equals(new ArrayList<>(expected.values())), "Sorted ticket collection changed order");
        }
        var copy = set.copy(); copy.remove(copy.first());
        check(copy.size() + 1 == set.size(), "Ticket snapshot shared its backing array");
        var iterator = set.iterator();
        while (iterator.hasNext()) { iterator.next(); iterator.remove(); }
        check(set.isEmpty(), "Iterator removal skipped tickets");
        var permanent = ticket(type, LEVEL, 1, Long.MIN_VALUE);
        var one = ticket(type, LEVEL, 2, 1); var two = ticket(type, LEVEL, 3, 2);
        set.add(permanent); set.add(one); set.add(two);
        Ticket[] removed = new Ticket[3];
        check(set.expireAndRemoveInto(removed) == 1 && removed[0] == one, "Expiry removed the wrong ticket");
        check(set.expireAndRemoveInto(removed) == 1 && removed[0] == two && set.first() == permanent, "Countdown or compaction failed");
    }

    private static void expiryAndRefresh() {
        var manager = new Manager(); TicketType type = type(2);
        check(manager.addTicketAtLevel(type, -65, 64, LEVEL, 1), "Ticket was not added");
        manager.verifyLevels(); manager.tick();
        check(!manager.addTicketAtLevel(type, -65, 64, LEVEL, 1), "Refresh counted as a new ticket");
        manager.tick(); check(manager.hasTickets(), "Refreshed ticket expired early");
        manager.tick(); manager.verifyLevels(); check(!manager.hasTickets(), "Expired ticket remained stored");
        nativeType(type).moonrise$setTimeout(0);
        manager.addTicketAtLevel(type, -65, 64, LEVEL, 1); manager.tick();
        check(manager.removeTicketAtLevel(type, -65, 64, LEVEL, 1), "Ticket removal failed");
        manager.processTicketUpdates(); check(manager.levels.containsKey(key(-65, 64)), "Removal skipped the one-tick UNKNOWN ticket");
        manager.tick(); manager.verifyLevels(); check(!manager.hasTickets(), "UNKNOWN ticket did not expire");
        nativeType(type).moonrise$setTimeout(2);
        manager.addTicketAtLevel(type, 0, 0, LEVEL, 1);
        nativeType(type).moonrise$setTimeout(0);
        manager.addTicketAtLevel(type, 0, 0, LEVEL, 1);
        manager.tick(); manager.tick(); check(manager.hasTickets(), "Finite-to-permanent refresh retained an expiry");
        nativeType(type).moonrise$setTimeout(1);
        manager.addTicketAtLevel(type, 0, 0, LEVEL, 1);
        manager.tick(); check(!manager.hasTickets(), "Permanent-to-finite refresh did not register expiry");
        nativeType(type).moonrise$setTimeout(0);
        manager.addTicketAtLevel(type, 0, 0, LEVEL, 1);
        var before = manager.blockTicketUpdates();
        try { manager.processTicketUpdates(); throw new AssertionError("Blocked ticket update was allowed"); }
        catch (IllegalStateException expected) { /* Unload callbacks cannot recursively process tickets. */ }
        finally { manager.unblockTicketUpdates(before); }
        manager.verifyLevels();
    }

    private static void countersAndAtomicOperations() {
        var manager = new Manager(); long pos = key(63, -1);
        manager.addTicketAtLevel(TicketType.FORCED, pos, LEVEL, null);
        check(manager.count(ChunkSystemTicketType.COUNTER_TYPE_FORCED, pos) == 1, "Forced ticket was not counted");
        manager.addTicketAtLevel(TicketType.FORCED, pos, LEVEL, null);
        check(manager.count(ChunkSystemTicketType.COUNTER_TYPE_FORCED, pos) == 1, "Refresh duplicated forced count");
        TicketType kept = ChunkSystemTicketType.create("keep", Comparator.<Integer>naturalOrder(), 0,
            TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
        manager.addAndRemoveTickets(pos, kept, LEVEL, 1, TicketType.FORCED, LEVEL, null);
        check(manager.count(ChunkSystemTicketType.COUNTER_TYPE_FORCED, pos) == 0, "Removed forced count remained");
        check(manager.count(ChunkSystemTicketType.COUNTER_TYPE_KEEP_DIMENSION_ACTIVE, pos) == 1, "Active-dimension count changed");
        check(!manager.addIfRemovedTicket(pos, kept, LEVEL, 2, kept, LEVEL, 99), "Conditional add succeeded without removal");
        check(manager.addIfRemovedTicket(pos, kept, LEVEL, 2, kept, LEVEL, 1), "Conditional replacement failed");
        manager.performTicketOperations(List.of(
            MoonriseTicketManager.TicketOperation.addOp(-65, 64, kept, LEVEL, 3),
            MoonriseTicketManager.TicketOperation.removeOp(pos, kept, LEVEL, 2)));
        manager.removeAllTicketsFor(kept, LEVEL, 3); manager.tick(); manager.verifyLevels();
        check(!manager.hasTickets(), "Bulk removal retained tickets");
    }

    private static void randomizedChanges() {
        var manager = new Manager(); TicketType type = type(0); var random = new Random(71931);
        int[] positions = {-65, -64, -1, 0, 63, 64, 128};
        for (int batch = 0; batch < 45; batch++) {
            for (int i = 0; i < 8; i++) {
                int x = positions[random.nextInt(positions.length)], z = positions[random.nextInt(positions.length)];
                int level = LEVEL + random.nextInt(5), id = random.nextInt(4);
                if (random.nextBoolean()) manager.addTicketAtLevel(type, x, z, level, id);
                else manager.removeTicketAtLevel(type, x, z, level, id);
            }
            manager.tick(); manager.verifyLevels();
        }
        for (int level = LEVEL; level < LEVEL + 5; level++) for (int id = 0; id < 4; id++) manager.removeAllTicketsFor(type, level, id);
        manager.tick(); manager.verifyLevels(); check(!manager.hasTickets(), "Randomized tickets were not cleaned up");
    }

    private static void pluginTickets() {
        var manager = new Manager();
        TicketType type = TicketTypeBridge.bridge$pluginTicket();
        var alpha = plugin("Alpha"); var beta = plugin("Beta"); var replacementAlpha = plugin("Alpha");
        manager.addTicketAtLevel(type, -65, 64, LEVEL, beta);
        manager.addTicketAtLevel(type, -65, 64, LEVEL, alpha);
        check(!manager.addTicketAtLevel(type, -65, 64, LEVEL, replacementAlpha), "Plugin name equality did not refresh its ticket");
        var plugins = manager.getPluginChunkTickets(-65, 64).stream().toList();
        check(plugins.size() == 2 && plugins.get(0) == replacementAlpha && plugins.get(1) == beta, "Plugin ticket keys or ordering changed");
        check(manager.count(ChunkSystemTicketType.COUNTER_TYPE_KEEP_DIMENSION_ACTIVE, key(-65, 64)) == 2,
            "Plugin refresh duplicated active-dimension count");
        manager.removeAllTicketsFor(type, LEVEL, alpha);
        check(manager.getPluginChunkTickets(-65, 64).size() == 1, "Plugin tickets could not be removed by equivalent name");
        manager.removeAllTicketsFor(type, LEVEL, beta); manager.tick(); manager.verifyLevels();
        check(!manager.hasTickets(), "Plugin ticket removal leaked storage");
    }

    private static org.bukkit.plugin.Plugin plugin(String name) {
        return (org.bukkit.plugin.Plugin) java.lang.reflect.Proxy.newProxyInstance(org.bukkit.plugin.Plugin.class.getClassLoader(),
            new Class<?>[] {org.bukkit.plugin.Plugin.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "getName", "toString" -> name;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private static void tickThreadDispatch() throws Exception {
        var manager = new Manager();
        manager.addTicketAtLevel(type(0), 0, 0, LEVEL, 1); manager.processTicketUpdates();
        check(!manager.pendingStatuses.isEmpty(), "Off-thread statuses were not delivered to the owner");
        var result = new java.util.concurrent.CompletableFuture<Boolean>();
        Thread thread = new TickThread(() -> {
            try { result.complete(manager.processTicketUpdates()); }
            catch (Throwable failure) { result.completeExceptionally(failure); }
        }, "ticket-verification-tick");
        thread.start();
        check(result.get(10, TimeUnit.SECONDS) && manager.pendingStatuses.isEmpty(), "Tick thread did not process pending statuses");
        thread.join(10_000); check(!thread.isAlive(), "Verification tick thread did not stop");
    }

    private static void concurrentChanges() throws Exception {
        var manager = new Manager(); TicketType type = type(0);
        var executor = Executors.newFixedThreadPool(4); var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<?>>();
        try {
            for (int worker = 0; worker < 4; worker++) {
                int id = worker;
                futures.add(executor.submit(() -> {
                    try { start.await(); } catch (InterruptedException failure) { throw new AssertionError(failure); }
                    for (int i = 0; i < 60; i++) {
                        int x = i % 5 * 64 - 65, z = id / 2 * 1024 - 65;
                        manager.addTicketAtLevel(type, x, z, LEVEL, id);
                        manager.processTicketUpdates();
                        manager.removeTicketAtLevel(type, x, z, LEVEL, id);
                    }
                }));
            }
            start.countDown(); for (var future : futures) future.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow(); check(executor.awaitTermination(10, TimeUnit.SECONDS), "Ticket workers did not stop");
        }
        manager.tick(); manager.verifyLevels(); check(!manager.hasTickets(), "Concurrent removal leaked tickets");
        check(manager.tasksStarted.get() > 0, "Ticket propagation did not dispatch tasks");
    }
    private static long key(int x, int z) { return (x & 0xffffffffL) | ((long) z << 32); }
    private static int x(long pos) { return (int) pos; }
    private static int z(long pos) { return (int) (pos >>> 32); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
