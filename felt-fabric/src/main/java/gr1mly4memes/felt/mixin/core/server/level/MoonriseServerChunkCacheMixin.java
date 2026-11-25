package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;

@Mixin(ServerChunkCache.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseServerChunkCacheMixin implements ca.spottedleaf.moonrise.patches.chunk_system.world.ChunkSystemServerChunkCache {
    @Shadow @Final public ServerLevel level;
    @Shadow @Final public ChunkMap chunkMap;
    @Shadow @Final public ServerChunkCache.MainThreadExecutor mainThreadProcessor;
    @Shadow @Final private java.util.Set<ChunkHolder> chunkHoldersToBroadcast;
    @Unique private final ca.spottedleaf.concurrentutil.map.concurrent.longs.ConcurrentChainedLong2ReferenceHashTable<LevelChunk> fullChunks = ca.spottedleaf.concurrentutil.map.concurrent.longs.ConcurrentChainedLong2ReferenceHashTable.createWithCapacity(128, 0.5f);

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;forEachBlockTickingChunk(Ljava/util/function/Consumer;)V"))
    private void felt$iterateTickingChunks(ChunkMap map, Consumer<LevelChunk> consumer,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (this.felt$scheduler() == null) { original.call(map, consumer); return; }
        int speed = this.level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED);
        var chunks = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getEntityTickingChunks();
        LevelChunk[] raw = chunks.getRawDataUnchecked();
        int size = chunks.size();
        java.util.Objects.checkFromToIndex(0, size, raw.length);
        for (int i = 0; i < size; ++i) {
            this.level.tickChunk(raw[i], speed);
            if ((i & 7) == 0) ((ca.spottedleaf.moonrise.patches.chunk_system.server.ChunkSystemMinecraftServer)this.level.getServer()).moonrise$executeMidTickTasks();
        }
    }
    @Override
    public final void moonrise$setFullChunk(final int chunkX, final int chunkZ, final LevelChunk chunk) {
        final long key = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkKey(chunkX, chunkZ);
        if (chunk == null) {
            this.fullChunks.remove(key);
        } else {
            this.fullChunks.put(key, chunk);
        }
    }

    @Override
    public final LevelChunk moonrise$getFullChunkIfLoaded(final int chunkX, final int chunkZ) {
        return this.fullChunks.get(ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkKey(chunkX, chunkZ));
    }

    @Override
    public void moonrise$chunkHolderUnload(final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder chunkHolder) {
        this.chunkHoldersToBroadcast.remove(chunkHolder.vanillaChunkHolder);
    }

    @Unique private ChunkAccess syncLoad(final int chunkX, final int chunkZ, final ChunkStatus toStatus) {
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler chunkTaskScheduler = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getChunkTaskScheduler();
        final CompletableFuture<ChunkAccess> completable = new CompletableFuture<>();
        chunkTaskScheduler.scheduleChunkLoad(
            chunkX, chunkZ, toStatus, true, ca.spottedleaf.concurrentutil.util.Priority.BLOCKING,
            completable::complete
        );

        if (!completable.isDone() && chunkTaskScheduler.hasShutdown()) {
            throw new IllegalStateException(
                "Chunk system has shut down, cannot process chunk requests in world '" + ca.spottedleaf.moonrise.common.util.WorldUtil.getWorldName(this.level) + "' at "
                    + "(" + chunkX + "," + chunkZ + ") status: " + toStatus
            );
        }

        if (ca.spottedleaf.moonrise.common.util.TickThread.isTickThreadFor(this.level, chunkX, chunkZ)) {
            ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.pushChunkWait(this.level, chunkX, chunkZ);
            this.mainThreadProcessor.managedBlock(completable::isDone);
            ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.popChunkWait();
        }

        final ChunkAccess ret = completable.join();
        if (ret == null) {
            throw new IllegalStateException("Chunk not loaded when requested");
        }

        return ret;
    }

    @Unique private ChunkAccess getChunkFallback(final int chunkX, final int chunkZ, final ChunkStatus toStatus,
                                         final boolean load) {
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler chunkTaskScheduler = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getChunkTaskScheduler();
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager chunkHolderManager = chunkTaskScheduler.chunkHolderManager;

        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder currentChunk = chunkHolderManager.getChunkHolder(ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkKey(chunkX, chunkZ));

        final ChunkAccess ifPresent = currentChunk == null ? null : currentChunk.getChunkIfPresent(toStatus);

        if (ifPresent != null && (toStatus != ChunkStatus.FULL || currentChunk.isFullChunkReady())) {
            return ifPresent;
        }

        final ca.spottedleaf.moonrise.common.PlatformHooks platformHooks = ca.spottedleaf.moonrise.common.PlatformHooks.get();

        if (platformHooks.hasCurrentlyLoadingChunk() && currentChunk != null) {
            final ChunkAccess loading = platformHooks.getCurrentlyLoadingChunk(currentChunk.vanillaChunkHolder);
            if (loading != null && ca.spottedleaf.moonrise.common.util.TickThread.isTickThread()) {
                return loading;
            }
        }

        return load ? this.syncLoad(chunkX, chunkZ, toStatus) : null;
    }

    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler felt$scheduler() {
        return ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getChunkTaskScheduler();
    }
    @Inject(method = "close()V", at = @At("HEAD"), cancellable = true)
    private void felt$close(CallbackInfo ci) throws java.io.IOException {
        if (this.felt$scheduler() == null) return;
        ((gr1mly4memes.felt.bridge.core.world.server.ServerChunkProviderBridge)(Object)this).bridge$close(true);
        ci.cancel();
    }
    @Inject(method = "getChunk", at = @At("HEAD"), cancellable = true)
    private void felt$get(int x, int z, ChunkStatus status, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
        if (this.felt$scheduler() == null) return;
        LevelChunk full = status == ChunkStatus.FULL ? this.moonrise$getFullChunkIfLoaded(x, z) : null;
        cir.setReturnValue(full != null ? full : this.getChunkFallback(x, z, status, create));
    }
    @Inject(method = "getChunkNow", at = @At("HEAD"), cancellable = true)
    private void felt$getNow(int x, int z, CallbackInfoReturnable<LevelChunk> cir) {
        if (this.felt$scheduler() == null) return;
        LevelChunk full = this.moonrise$getFullChunkIfLoaded(x, z);
        if (full == null && ca.spottedleaf.moonrise.common.util.TickThread.isTickThread() && ca.spottedleaf.moonrise.common.PlatformHooks.get().hasCurrentlyLoadingChunk()) {
            var holder = this.felt$scheduler().chunkHolderManager.getChunkHolder(x, z);
            if (holder != null) full = ca.spottedleaf.moonrise.common.PlatformHooks.get().getCurrentlyLoadingChunk(holder.vanillaChunkHolder);
        }
        cir.setReturnValue(full);
    }
    @Inject(method = "getFullChunk", at = @At("HEAD"), cancellable = true)
    private void felt$spawnCountingChunk(long key, Consumer<LevelChunk> output, CallbackInfo ci) {
        if (this.felt$scheduler() == null) return;
        // NaturalSpawner uses this query to count existing mobs. The vanilla
        // holder futures are not populated by the native chunk system.
        LevelChunk chunk = this.fullChunks.get(key);
        if (chunk != null) output.accept(chunk);
        ci.cancel();
    }
    @Inject(method = "getChunkFutureMainThread", at = @At("HEAD"), cancellable = true)
    private void felt$future(int x, int z, ChunkStatus status, boolean create, CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> cir) {
        if (this.felt$scheduler() != null) cir.setReturnValue(this.felt$getChunkFutureInternal(x, z, status, create, false));
    }
    @Inject(method = "getChunkForLighting", at = @At("HEAD"), cancellable = true)
    private void felt$light(int x, int z, CallbackInfoReturnable<LightChunk> cir) {
        if (this.felt$scheduler() == null) return;
        var holder = this.felt$scheduler().chunkHolderManager.getChunkHolder(x, z);
        cir.setReturnValue(holder == null ? null : holder.getChunkIfPresentUnchecked(ChunkStatus.INITIALIZE_LIGHT.getParent()));
    }
    @Inject(method = "hasChunk", at = @At("HEAD"), cancellable = true)
    private void felt$has(int x, int z, CallbackInfoReturnable<Boolean> cir) { if (this.felt$scheduler() != null) cir.setReturnValue(((ServerChunkCache)(Object)this).getChunkNow(x, z) != null); }
    @Inject(method = "runDistanceManagerUpdates", at = @At("HEAD"), cancellable = true)
    private void felt$update(CallbackInfoReturnable<Boolean> cir) { if (this.felt$scheduler() != null) cir.setReturnValue(this.felt$scheduler().chunkHolderManager.processTicketUpdates()); }
    @Inject(method = "tick", at = @At("HEAD"))
    private void felt$players(java.util.function.BooleanSupplier haveTime, boolean tickChunks, CallbackInfo ci) { if (tickChunks && this.felt$scheduler() != null) ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getPlayerChunkLoader().tick(); }
    @Unique private CompletableFuture<ChunkResult<ChunkAccess>> felt$getChunkFutureInternal(
        final int x, final int z, final ChunkStatus targetStatus, final boolean loadOrGenerate, final boolean intendingToBlock
    ) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.level, x, z, "Scheduling chunk load off-main");

        final int minLevel = ChunkLevel.byStatus(targetStatus);
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder chunkHolder = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(x, z);

        final boolean needsFullScheduling = targetStatus == ChunkStatus.FULL && (chunkHolder == null || !chunkHolder.getChunkStatus().isOrAfter(FullChunkStatus.FULL));

        if ((chunkHolder == null || chunkHolder.getTicketLevel() > minLevel || needsFullScheduling) && !loadOrGenerate) {
            return ChunkHolder.UNLOADED_CHUNK_FUTURE;
        }

        final ChunkAccess ifPresent = chunkHolder == null ? null : chunkHolder.getChunkIfPresent(targetStatus);
        if (needsFullScheduling || ifPresent == null) {
            // schedule
            final CompletableFuture<ChunkResult<ChunkAccess>> ret = new CompletableFuture<>();
            final Consumer<ChunkAccess> complete = (ChunkAccess chunk) -> {
                if (chunk == null) {
                    ret.complete(ChunkHolder.UNLOADED_CHUNK);
                } else {
                    ret.complete(ChunkResult.of(chunk));
                }
            };

            ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getChunkTaskScheduler().scheduleChunkLoad(
                x, z, targetStatus, true,
                intendingToBlock ? ca.spottedleaf.concurrentutil.util.Priority.BLOCKING : ca.spottedleaf.concurrentutil.util.Priority.NORMAL,
                complete
            );

            return ret;
        } else {
            // can return now
            return CompletableFuture.completedFuture(ChunkResult.of(ifPresent));
        }
    }

}
