package gr1mly4memes.felt.mixin.core.server;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.ServerLevel;
@Mixin(net.minecraft.server.MinecraftServer.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseMinecraftServerMixin implements ca.spottedleaf.moonrise.patches.chunk_system.server.ChunkSystemMinecraftServer {
    @Shadow public abstract Iterable<ServerLevel> getAllLevels();
    @Shadow public abstract net.minecraft.server.players.PlayerList getPlayerList();
    @Unique private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    @Unique private volatile Throwable chunkSystemCrash;

    @Override
    public final void moonrise$setChunkSystemCrash(final Throwable throwable) {
        this.chunkSystemCrash = throwable;
    }

    @Unique private static final long CHUNK_TASK_QUEUE_BACKOFF_MIN_TIME = 25L * 1000L; // 25us
    @Unique private static final long MAX_CHUNK_EXEC_TIME = 1000L; // 1us
    @Unique private static final long TASK_EXECUTION_FAILURE_BACKOFF = 5L * 1000L; // 5us

    @Unique private long lastMidTickExecute;
    @Unique private long lastMidTickExecuteFailure;

    @Unique private boolean tickMidTickTasks() {
        // give all worlds a fair chance at by targeting them all.
        // if we execute too many tasks, that's fine - we have logic to correctly handle overuse of allocated time.
        boolean executed = false;
        for (final ServerLevel world : this.getAllLevels()) {
            long currTime = System.nanoTime();
            if (currTime - ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getLastMidTickFailure() <= TASK_EXECUTION_FAILURE_BACKOFF) {
                continue;
            }
            if (!world.getChunkSource().pollTask()) {
                // we need to back off if this fails
                ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$setLastMidTickFailure(currTime);
            } else {
                executed = true;
            }
        }

        return executed;
    }

    @Override
    public final void moonrise$executeMidTickTasks() {
        final long startTime = System.nanoTime();
        if ((startTime - this.lastMidTickExecute) <= CHUNK_TASK_QUEUE_BACKOFF_MIN_TIME || (startTime - this.lastMidTickExecuteFailure) <= TASK_EXECUTION_FAILURE_BACKOFF) {
            // it's shown to be bad to constantly hit the queue (chunk loads slow to a crawl), even if no tasks are executed.
            // so, backoff to prevent this
            return;
        }

        for (;;) {
            final boolean moreTasks = this.tickMidTickTasks();
            final long currTime = System.nanoTime();
            final long diff = currTime - startTime;

            if (!moreTasks || diff >= MAX_CHUNK_EXEC_TIME) {
                if (!moreTasks) {
                    this.lastMidTickExecuteFailure = currTime;
                }

                // note: negative values reduce the time
                long overuse = diff - MAX_CHUNK_EXEC_TIME;
                if (overuse >= (10L * 1000L * 1000L)) { // 10ms
                    // make sure something like a GC or dumb plugin doesn't screw us over...
                    overuse = 10L * 1000L * 1000L; // 10ms
                }

                final double overuseCount = (double)overuse/(double)MAX_CHUNK_EXEC_TIME;
                final long extraSleep = (long)Math.round(overuseCount*CHUNK_TASK_QUEUE_BACKOFF_MIN_TIME);

                this.lastMidTickExecute = currTime + extraSleep;
                return;
            }
        }
    }

    @Override
    public final void moonrise$issueEmergencySave() {
        LOGGER.warn("Performing emergency save...");
        LOGGER.info("Saving all players...");
        this.getPlayerList().saveAll();
        LOGGER.info("Saved all players");
        LOGGER.info("Saving all worlds...");
        for (final ServerLevel world : this.getAllLevels()) {
            LOGGER.info("Saving chunks in world '" + ca.spottedleaf.moonrise.common.util.WorldUtil.getWorldName(world) + "'...");
            ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$issueEmergencySave();
            LOGGER.info("Saved chunks in world '" + ca.spottedleaf.moonrise.common.util.WorldUtil.getWorldName(world) + "'...");
        }
        LOGGER.info("Saved all worlds");
        LOGGER.warn("Performed emergency save");
    }

    @Redirect(method = "spin", at = @At(value = "NEW", target = "(Ljava/lang/Runnable;Ljava/lang/String;)Ljava/lang/Thread;"))
    private static Thread felt$tickThread(Runnable run, String name) {
        return new ca.spottedleaf.moonrise.common.util.TickThread(run, name);
    }
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void felt$crash(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) {
        Throwable failure = this.chunkSystemCrash;
        if (failure != null) { this.chunkSystemCrash = null; throw new RuntimeException("Chunk system crash propagated to tick()", failure); }
    }
    @Inject(method = "tickChildren", at = @At("RETURN"))
    private void felt$drain(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) {
        for (ServerLevel level : this.getAllLevels()) {
            var scheduler = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)level).moonrise$getChunkTaskScheduler();
            scheduler.chunkHolderManager.processTicketUpdates();
            scheduler.executeAllRecentlyQueuedMainThreadTasks();
        }
    }
    @Inject(method = "stopServer", at = @At("RETURN"))
    private void felt$shutdown(CallbackInfo ci) {
        ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.flush((net.minecraft.server.MinecraftServer)(Object)this);
        if ((Object)this instanceof net.minecraft.server.dedicated.DedicatedServer) {
            ca.spottedleaf.moonrise.common.util.MoonriseCommon.haltExecutors();
            io.papermc.paper.util.MCUtil.ASYNC_EXECUTOR.shutdown();
        }
    }

}
