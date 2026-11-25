package gr1mly4memes.felt;

import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.queue.ChunkUnloadQueue;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.PriorityHolder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Tests the actual pinned upstream implementations, without booting a world. */
public final class MoonriseSchedulingPrimitivesTest {
    private static final class Holder extends PriorityHolder {
        final AtomicInteger scheduled = new AtomicInteger();
        final AtomicInteger cancelled = new AtomicInteger();
        volatile Priority taskPriority;
        Holder() { super(Priority.NORMAL); }
        @Override protected void cancelScheduled() { cancelled.incrementAndGet(); }
        @Override protected Priority getScheduledPriority() { return taskPriority; }
        @Override protected void scheduleTask(Priority priority) { taskPriority = priority; scheduled.incrementAndGet(); }
        @Override protected void lowerPriorityScheduled(Priority priority) {
            if (priority.isLowerPriority(taskPriority.priority)) taskPriority = priority;
        }
        @Override protected void setPriorityScheduled(Priority priority) { taskPriority = priority; }
        @Override protected void raisePriorityScheduled(Priority priority) {
            if (priority.isHigherPriority(taskPriority.priority)) taskPriority = priority;
        }
    }

    public static void main(String[] args) throws Exception {
        priorityLifecycle();
        executionRaces();
        unloadOrdering();
        System.out.println("Moonrise primitives: priorities, cancellation races, execution ownership and unload ordering passed");
    }

    private static void priorityLifecycle() {
        Holder holder = new Holder();
        holder.raisePriority(Priority.HIGHEST);
        check(holder.getPriority() == Priority.HIGHEST, "Pending promotion was lost");
        holder.schedule();
        check(holder.isScheduled() && holder.scheduled.get() == 1, "Task was not scheduled once");
        holder.setPriority(Priority.NORMAL);
        check(holder.getPriority() == Priority.NORMAL, "Scheduled priority was not updated");
        holder.cancel(); holder.cancel();
        check(holder.cancelled.get() == 1 && !holder.markExecuting(), "Cancellation did not prevent execution");
        check(holder.getPriority() == Priority.COMPLETING, "Terminal priority changed");
        Holder cancelledBeforeSchedule = new Holder();
        cancelledBeforeSchedule.cancel(); cancelledBeforeSchedule.schedule();
        check(cancelledBeforeSchedule.scheduled.get() == 0, "Cancelled task was queued");
        Holder duplicate = new Holder(); duplicate.schedule();
        try { duplicate.schedule(); throw new AssertionError("Duplicate scheduling was accepted"); }
        catch (IllegalStateException expected) { /* Upstream rejects duplicate scheduling. */ }
    }

    private static void executionRaces() throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            Holder holder = new Holder();
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger owners = new AtomicInteger();
            Runnable run = () -> {
                try { start.await(); } catch (InterruptedException failure) { throw new AssertionError(failure); }
                if (holder.markExecuting()) owners.incrementAndGet();
            };
            Thread first = new Thread(run); Thread second = new Thread(run);
            first.start(); second.start(); start.countDown(); first.join(); second.join();
            check(owners.get() == 1, "More than one worker acquired execution");
            holder.cancel();
            check(holder.cancelled.get() == 0, "Cancellation ran after execution was acquired");
        }
    }

    private static void unloadOrdering() {
        ChunkUnloadQueue queue = new ChunkUnloadQueue(4);
        check(queue.addChunk(0, 0) && queue.addChunk(1, 1), "Same-section insertion failed");
        check(!queue.addChunk(0, 0), "Duplicate chunk was queued");
        queue.addChunk(16, -1); queue.addChunk(-1, 16);
        var sections = queue.retrieveForAllRegions();
        check(sections.size() == 3 && sections.getFirst().count() == 2, "Unload section grouping changed");
        check(sections.get(1).sectionX() == 1 && sections.get(1).sectionZ() == -1, "Negative Z was grouped incorrectly");
        check(sections.get(2).sectionX() == -1 && sections.get(2).sectionZ() == 1, "Negative X was grouped incorrectly");
        check(sections.get(0).order() < sections.get(1).order() && sections.get(1).order() < sections.get(2).order(), "FIFO order changed");
        check(queue.removeChunk(0, 0) && queue.removeChunk(1, 1), "Unload cancellation failed");
        check(queue.getSectionUnsynchronized(0, 0) == null, "Empty unload section was retained");
        queue.addChunk(0, 0);
        check(queue.retrieveForAllRegions().getLast().sectionX() == 0, "Recreated section retained stale FIFO position");
        check(queue.removeSection(1, -1) != null && queue.getSectionUnsynchronized(1, -1) == null, "Section removal failed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
