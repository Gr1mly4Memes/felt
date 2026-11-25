package gr1mly4memes.felt.mixin.core.server.level;

import ca.spottedleaf.moonrise.patches.chunk_system.level.*;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = ServerLevel.class, priority = 900)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseServerLevelMixin implements ChunkSystemServerLevel, ChunkSystemLevelReader, ca.spottedleaf.moonrise.patches.chunk_tick_iteration.ChunkTickServerLevel {
    @Shadow @Final private ServerChunkCache chunkSource;
    @Shadow @Final private MinecraftServer server;
    @Unique private final ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.ViewDistanceHolder viewDistanceHolder = new ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.ViewDistanceHolder();
    @Unique private final ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader chunkLoader = new ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader((ServerLevel)(Object)this);
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.EntityDataController entityDataController;
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.PoiDataController poiDataController;
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.ChunkDataController chunkDataController;
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler chunkTaskScheduler;
    @Unique private long lastMidTickFailure;
    @Unique private long tickedBlocksOrFluids;
    @Unique private final ca.spottedleaf.moonrise.common.misc.NearbyPlayers nearbyPlayers = new ca.spottedleaf.moonrise.common.misc.NearbyPlayers((ServerLevel)(Object)this);
    @Unique private static final LevelChunk[] EMPTY_LEVEL_CHUNKS = new LevelChunk[0];
    @Unique private final ca.spottedleaf.moonrise.common.list.ReferenceList<LevelChunk> loadedChunks = new ca.spottedleaf.moonrise.common.list.ReferenceList<>(EMPTY_LEVEL_CHUNKS);
    @Unique private final ca.spottedleaf.moonrise.common.list.ReferenceList<LevelChunk> tickingChunks = new ca.spottedleaf.moonrise.common.list.ReferenceList<>(EMPTY_LEVEL_CHUNKS);
    @Unique private final ca.spottedleaf.moonrise.common.list.ReferenceList<LevelChunk> entityTickingChunks = new ca.spottedleaf.moonrise.common.list.ReferenceList<>(EMPTY_LEVEL_CHUNKS);

    @Override
    public LevelChunk moonrise$getFullChunkIfLoaded(final int chunkX, final int chunkZ) {
        return this.chunkSource.getChunkNow(chunkX, chunkZ);
    }

    @Override
    public ChunkAccess moonrise$getAnyChunkIfLoaded(final int chunkX, final int chunkZ) {
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder newChunkHolder = this.moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkKey(chunkX, chunkZ));
        if (newChunkHolder == null) {
            return null;
        }
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder.ChunkCompletion lastCompletion = newChunkHolder.getLastChunkCompletion();
        return lastCompletion == null ? null : lastCompletion.chunk();
    }

    @Override
    public ChunkAccess moonrise$getSpecificChunkIfLoaded(final int chunkX, final int chunkZ, final net.minecraft.world.level.chunk.status.ChunkStatus leastStatus) {
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder newChunkHolder = this.moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(chunkX, chunkZ);
        if (newChunkHolder == null) {
            return null;
        }
        return newChunkHolder.getChunkIfPresentUnchecked(leastStatus);
    }

    @Override
    public void moonrise$midTickTasks() {
        ((ca.spottedleaf.moonrise.patches.chunk_system.server.ChunkSystemMinecraftServer)this.server).moonrise$executeMidTickTasks();
    }

    @Override
    public ChunkAccess moonrise$syncLoadNonFull(final int chunkX, final int chunkZ, final net.minecraft.world.level.chunk.status.ChunkStatus status) {
        return this.moonrise$getChunkTaskScheduler().syncLoadNonFull(chunkX, chunkZ, status);
    }

    @Override
    public ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler moonrise$getChunkTaskScheduler() {
        return this.chunkTaskScheduler;
    }

    @Override
    public ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionDataController  moonrise$getChunkDataController() {
        return this.chunkDataController;
    }

    @Override
    public ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionDataController moonrise$getPoiChunkDataController() {
        return this.poiDataController;
    }

    @Override
    public ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionDataController moonrise$getEntityChunkDataController() {
        return this.entityDataController;
    }

    @Override
    public int moonrise$getRegionChunkShift() {
        return ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ThreadedTicketLevelPropagator.SECTION_SHIFT;
    }

    @Override
    public ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader moonrise$getPlayerChunkLoader() {
        return this.chunkLoader;
    }

    @Override
    public void moonrise$loadChunksAsync(final BlockPos pos, final int radiusBlocks,
                                               final ca.spottedleaf.concurrentutil.util.Priority priority,
                                               final java.util.function.Consumer<java.util.List<net.minecraft.world.level.chunk.ChunkAccess>> onLoad) {
        this.moonrise$loadChunksAsync(
            (pos.getX() - radiusBlocks) >> 4,
            (pos.getX() + radiusBlocks) >> 4,
            (pos.getZ() - radiusBlocks) >> 4,
            (pos.getZ() + radiusBlocks) >> 4,
            priority, onLoad
        );
    }

    @Override
    public void moonrise$loadChunksAsync(final BlockPos pos, final int radiusBlocks,
                                               final net.minecraft.world.level.chunk.status.ChunkStatus chunkStatus, final ca.spottedleaf.concurrentutil.util.Priority priority,
                                               final java.util.function.Consumer<java.util.List<net.minecraft.world.level.chunk.ChunkAccess>> onLoad) {
        this.moonrise$loadChunksAsync(
            (pos.getX() - radiusBlocks) >> 4,
            (pos.getX() + radiusBlocks) >> 4,
            (pos.getZ() - radiusBlocks) >> 4,
            (pos.getZ() + radiusBlocks) >> 4,
            chunkStatus, priority, onLoad
        );
    }

    @Override
    public void moonrise$loadChunksAsync(final int minChunkX, final int maxChunkX, final int minChunkZ, final int maxChunkZ,
                                               final ca.spottedleaf.concurrentutil.util.Priority priority,
                                               final java.util.function.Consumer<java.util.List<net.minecraft.world.level.chunk.ChunkAccess>> onLoad) {
        this.moonrise$loadChunksAsync(minChunkX, maxChunkX, minChunkZ, maxChunkZ, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, priority, onLoad);
    }

    @Override
    public void moonrise$loadChunksAsync(final int minChunkX, final int maxChunkX, final int minChunkZ, final int maxChunkZ,
                                               final net.minecraft.world.level.chunk.status.ChunkStatus chunkStatus, final ca.spottedleaf.concurrentutil.util.Priority priority,
                                               final java.util.function.Consumer<java.util.List<net.minecraft.world.level.chunk.ChunkAccess>> onLoad) {
        this.moonrise$loadChunksAsync(minChunkX, maxChunkX, minChunkZ, maxChunkZ, chunkStatus, priority, onLoad, null);
    }

    @Override
    public void moonrise$loadChunksAsync(final int minChunkX, final int maxChunkX, final int minChunkZ, final int maxChunkZ,
                                               final net.minecraft.world.level.chunk.status.ChunkStatus chunkStatus, final ca.spottedleaf.concurrentutil.util.Priority priority,
                                               final java.util.function.Consumer<java.util.List<net.minecraft.world.level.chunk.ChunkAccess>> onLoad, final java.util.function.Consumer<net.minecraft.world.level.chunk.ChunkAccess> onEachLoad) {
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler chunkTaskScheduler = this.moonrise$getChunkTaskScheduler();
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager chunkHolderManager = chunkTaskScheduler.chunkHolderManager;

        final int requiredChunks = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        final java.util.concurrent.atomic.AtomicInteger loadedChunks = new java.util.concurrent.atomic.AtomicInteger();
        final Long holderIdentifier = ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.getNextChunkLoadId();
        final int ticketLevel = ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.getTicketLevel(chunkStatus);

        final List<ChunkAccess> ret = new ArrayList<>(requiredChunks);

        final java.util.function.Consumer<net.minecraft.world.level.chunk.ChunkAccess> consumer = (final ChunkAccess chunk) -> {
            if (chunk != null) {
                synchronized (ret) {
                    ret.add(chunk);
                }
                chunkHolderManager.addTicketAtLevel(ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.CHUNK_LOAD, chunk.getPos(), ticketLevel, holderIdentifier);
            }
            if (onEachLoad != null) {
                onEachLoad.accept(chunk);
            }
            if (loadedChunks.incrementAndGet() == requiredChunks) {
                try {
                    if (onLoad != null) {
                        onLoad.accept(java.util.Collections.unmodifiableList(ret));
                    }
                } finally {
                    for (int i = 0, len = ret.size(); i < len; ++i) {
                        final ChunkPos chunkPos = ret.get(i).getPos();

                        chunkHolderManager.removeTicketAtLevel(ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.CHUNK_LOAD, chunkPos, ticketLevel, holderIdentifier);
                    }
                }
            }
        };

        for (int cx = minChunkX; cx <= maxChunkX; ++cx) {
            for (int cz = minChunkZ; cz <= maxChunkZ; ++cz) {
                ca.spottedleaf.moonrise.common.PlatformHooks.get().scheduleChunkLoad(
                    (ServerLevel)(Object)this, cx, cz, chunkStatus, true, priority, consumer
                );
            }
        }
    }

    @Override
    public ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.ViewDistanceHolder moonrise$getViewDistanceHolder() {
        return this.viewDistanceHolder;
    }

    @Override
    public long moonrise$getLastMidTickFailure() {
        return this.lastMidTickFailure;
    }

    @Override
    public void moonrise$setLastMidTickFailure(final long time) {
        this.lastMidTickFailure = time;
    }

    @Override
    public ca.spottedleaf.moonrise.common.misc.NearbyPlayers moonrise$getNearbyPlayers() {
        return this.nearbyPlayers;
    }

    @Override
    public ca.spottedleaf.moonrise.common.list.ReferenceList<LevelChunk> moonrise$getLoadedChunks() {
        return this.loadedChunks;
    }

    @Override
    public ca.spottedleaf.moonrise.common.list.ReferenceList<LevelChunk> moonrise$getTickingChunks() {
        return this.tickingChunks;
    }

    @Override
    public ca.spottedleaf.moonrise.common.list.ReferenceList<LevelChunk> moonrise$getEntityTickingChunks() {
        return this.entityTickingChunks;
    }

    @Override
    public boolean moonrise$areChunksLoaded(final int fromX, final int fromZ, final int toX, final int toZ) {
        final ServerChunkCache chunkSource = this.chunkSource;

        for (int currZ = fromZ; currZ <= toZ; ++currZ) {
            for (int currX = fromX; currX <= toX; ++currX) {
                if (!chunkSource.hasChunk(currX, currZ)) {
                    return false;
                }
            }
        }

        return true;
    }

   @Override
   public final void moonrise$issueEmergencySave() {
       this.moonrise$getChunkTaskScheduler().chunkHolderManager.saveAllChunks(
           true, true, true, true
       );
   }
    // Paper end - rewrite chunk system
    // Paper start - chunk tick iteration
    @Unique private final ca.spottedleaf.moonrise.common.list.ReferenceList<LevelChunk> playerTickingChunks = new ca.spottedleaf.moonrise.common.list.ReferenceList<>(EMPTY_LEVEL_CHUNKS);
    @Unique private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap playerTickingRequests = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();

    @Override
    public ca.spottedleaf.moonrise.common.list.ReferenceList<LevelChunk> moonrise$getPlayerTickingChunks() {
        return this.playerTickingChunks;
    }

    @Override
    public void moonrise$markChunkForPlayerTicking(final LevelChunk chunk) {
        final ChunkPos pos = chunk.getPos();
        if (!this.playerTickingRequests.containsKey(ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkKey(pos))) {
            return;
        }

        this.playerTickingChunks.add(chunk);
    }

    @Override
    public void moonrise$removeChunkForPlayerTicking(final LevelChunk chunk) {
        this.playerTickingChunks.remove(chunk);
    }

    @Override
    public void moonrise$addPlayerTickingRequest(final int chunkX, final int chunkZ) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread((ServerLevel)(Object)this, chunkX, chunkZ, "Cannot add ticking request async");

        final long chunkKey = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkKey(chunkX, chunkZ);

        if (this.playerTickingRequests.addTo(chunkKey, 1) != 0) {
            // already added
            return;
        }

        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder chunkHolder = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)(ServerLevel)(Object)this).moonrise$getChunkTaskScheduler()
            .chunkHolderManager.getChunkHolder(chunkKey);

        if (chunkHolder == null || !chunkHolder.isTickingReady()) {
            return;
        }

        this.playerTickingChunks.add((LevelChunk)chunkHolder.getCurrentChunk());
    }

    @Override
    public void moonrise$removePlayerTickingRequest(final int chunkX, final int chunkZ) {
        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread((ServerLevel)(Object)this, chunkX, chunkZ, "Cannot remove ticking request async");

        final long chunkKey = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkKey(chunkX, chunkZ);
        final int val = this.playerTickingRequests.addTo(chunkKey, -1);

        if (val <= 0) {
            throw new IllegalStateException("Negative counter");
        }

        if (val != 1) {
            // still has at least one request
            return;
        }
        this.playerTickingRequests.remove(chunkKey);

        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder chunkHolder = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)(ServerLevel)(Object)this).moonrise$getChunkTaskScheduler()
            .chunkHolderManager.getChunkHolder(chunkKey);

        if (chunkHolder == null || !chunkHolder.isTickingReady()) {
            return;
        }

        this.playerTickingChunks.remove((LevelChunk)chunkHolder.getCurrentChunk());
    }


    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$init(MinecraftServer server, java.util.concurrent.Executor executor,
        net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess storage,
        net.minecraft.world.level.storage.ServerLevelData data,
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
        net.minecraft.world.level.dimension.LevelStem stem, boolean debug, long seed,
        List<net.minecraft.world.level.CustomSpawner> spawners, boolean tickTime, CallbackInfo ci) {
        ServerLevel world = (ServerLevel)(Object)this;
        this.moonrise$setEntityLookup(new ca.spottedleaf.moonrise.patches.chunk_system.level.entity.server.ServerEntityLookup(world, world.new EntityCallbacks()));
        this.chunkTaskScheduler = new ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler(world);
        this.entityDataController = new ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.EntityDataController(
            new ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.EntityDataController.EntityRegionFileStorage(
                new net.minecraft.world.level.chunk.storage.RegionStorageInfo(storage.getLevelId(), dimension, "entities"), storage.getDimensionPath(dimension).resolve("entities"), server.forceSynchronousWrites()), this.chunkTaskScheduler);
        this.poiDataController = new ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.PoiDataController(world, this.chunkTaskScheduler);
        this.chunkDataController = new ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.ChunkDataController(world, this.chunkTaskScheduler);
        this.chunkLoader.setLoadDistance(world.getChunkSource().chunkMap.serverViewDistance + 1);
        this.chunkLoader.setTickDistance(world.getChunkSource().chunkMap.getDistanceManager().simulationDistance);
    }
    @Inject(method = "getEntities()Lnet/minecraft/world/level/entity/LevelEntityGetter;", at = @At("HEAD"), cancellable = true)
    private void felt$entities(CallbackInfoReturnable<net.minecraft.world.level.entity.LevelEntityGetter<net.minecraft.world.entity.Entity>> cir) {
        if (this.moonrise$getEntityLookup() != null) cir.setReturnValue(this.moonrise$getEntityLookup());
    }
    @Redirect(method = {"addPlayer", "addEntity"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private boolean felt$add(net.minecraft.world.level.entity.PersistentEntitySectionManager<net.minecraft.world.entity.Entity> old, net.minecraft.world.level.entity.EntityAccess entity) {
        return this.moonrise$getEntityLookup().addNewEntity((net.minecraft.world.entity.Entity)entity);
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;tick()V"))
    private void felt$noLegacyTick(net.minecraft.world.level.entity.PersistentEntitySectionManager<?> old) {}
    @Redirect(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;saveAll()V"))
    private void felt$noLegacySave(net.minecraft.world.level.entity.PersistentEntitySectionManager<?> old) {}
    @Redirect(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;autoSave()V"))
    private void felt$noLegacyAutoSave(net.minecraft.world.level.entity.PersistentEntitySectionManager<?> old) {}
    @Inject(method = "addLegacyChunkEntities", at = @At("HEAD"), cancellable = true)
    private void felt$legacy(java.util.stream.Stream<net.minecraft.world.entity.Entity> entities, CallbackInfo ci) {
        List<net.minecraft.world.entity.Entity> list = entities.toList();
        if (!list.isEmpty()) this.moonrise$getEntityLookup().addLegacyChunkEntities(list, null);
        ci.cancel();
    }
    @Inject(method = "addWorldGenChunkEntities", at = @At("HEAD"), cancellable = true)
    private void felt$generated(java.util.stream.Stream<net.minecraft.world.entity.Entity> entities, CallbackInfo ci) {
        List<net.minecraft.world.entity.Entity> list = entities.toList();
        if (!list.isEmpty()) this.moonrise$getEntityLookup().addWorldGenChunkEntities(list, null);
        ci.cancel();
    }
    @Inject(method = "shouldTickBlocksAt", at = @At("HEAD"), cancellable = true)
    private void felt$shouldTick(long pos, CallbackInfoReturnable<Boolean> cir) {
        var holder = this.chunkTaskScheduler.chunkHolderManager.getChunkHolder(pos);
        cir.setReturnValue(holder != null && holder.isTickingReady());
    }
    @Inject(method = "areEntitiesLoaded", at = @At("HEAD"), cancellable = true)
    private void felt$loaded(long pos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.moonrise$getAnyChunkIfLoaded(ChunkPos.unpack(pos).x(), ChunkPos.unpack(pos).z()) != null);
    }
    @Inject(method = "isPositionTickingWithEntitiesLoaded", at = @At("HEAD"), cancellable = true)
    private void felt$blockTicking(long pos, CallbackInfoReturnable<Boolean> cir) {
        var holder = this.chunkTaskScheduler.chunkHolderManager.getChunkHolder(pos);
        cir.setReturnValue(holder != null && holder.isTickingReady());
    }
    @Inject(method = "isPositionEntityTicking", at = @At("HEAD"), cancellable = true)
    private void felt$entityTicking(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var holder = this.chunkTaskScheduler.chunkHolderManager.getChunkHolder(pos.getX() >> 4, pos.getZ() >> 4);
        cir.setReturnValue(holder != null && holder.isEntityTickingReady());
    }
    @Inject(method = "canSpawnEntitiesInChunk", at = @At("HEAD"), cancellable = true)
    private void felt$spawnTicking(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        var holder = this.chunkTaskScheduler.chunkHolderManager.getChunkHolder(pos.pack());
        cir.setReturnValue(holder != null && holder.isEntityTickingReady() && ((ServerLevel)(Object)this).getWorldBorder().isWithinBounds(pos));
    }
    @Inject(method = {"tickBlock", "tickFluid"}, at = @At("RETURN"))
    private void felt$midTick(CallbackInfo ci) {
        if ((++this.tickedBlocksOrFluids & 7L) == 0L) this.moonrise$midTickTasks();
    }
    @Inject(method = "areEntitiesActuallyLoadedAndTicking", at = @At("HEAD"), cancellable = true)
    private void felt$loadedTicking(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        var holder = this.chunkTaskScheduler.chunkHolderManager.getChunkHolder(pos.pack());
        cir.setReturnValue(holder != null && holder.isEntityTickingReady());
    }
}
