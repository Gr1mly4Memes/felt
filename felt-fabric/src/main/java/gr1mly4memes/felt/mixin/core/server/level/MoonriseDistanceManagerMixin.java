package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.*;
import net.minecraft.core.SectionPos;
@Mixin(DistanceManager.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseDistanceManagerMixin implements ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemDistanceManager, ca.spottedleaf.moonrise.patches.chunk_tick_iteration.ChunkTickDistanceManager {
    @Shadow @Final private net.minecraft.world.level.TicketStorage ticketStorage;
    @Shadow private int simulationDistance;
    @Override public ChunkMap moonrise$getChunkMap() { return ((ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketStorage)this.ticketStorage).moonrise$getChunkMap(); }
    @Override public ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager moonrise$getChunkHolderManager() {
        return ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.moonrise$getChunkMap().level).moonrise$getChunkTaskScheduler().chunkHolderManager;
    }
    @Unique private boolean felt$ready() { return this.moonrise$getChunkMap() != null && ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.moonrise$getChunkMap().level).moonrise$getChunkTaskScheduler() != null; }
    @Unique private final ca.spottedleaf.moonrise.common.misc.PositionCountingAreaMap<ServerPlayer> spawnChunkTracker = new ca.spottedleaf.moonrise.common.misc.PositionCountingAreaMap<>();
    // Note: Cannot do narrow tracking on Paper due to custom spawn range

    @Override
    public final void moonrise$addPlayer(final ServerPlayer player, final SectionPos pos) {
        this.spawnChunkTracker.add(player, pos.x(), pos.z(), ca.spottedleaf.moonrise.patches.chunk_tick_iteration.ChunkTickConstants.PLAYER_SPAWN_TRACK_RANGE);
        // Note: Cannot do narrow tracking on Paper due to custom spawn range
    }

    @Override
    public final void moonrise$removePlayer(final ServerPlayer player, final SectionPos pos) {
        this.spawnChunkTracker.remove(player);
        // Note: Cannot do narrow tracking on Paper due to custom spawn range
    }

    @Override
    public final void moonrise$updatePlayer(final ServerPlayer player,
                                            final SectionPos oldPos, final SectionPos newPos,
                                            final boolean oldIgnore, final boolean newIgnore) {
        if (newIgnore) {
            this.spawnChunkTracker.remove(player);
            // Note: Cannot do narrow tracking on Paper due to custom spawn range
        } else {
            this.spawnChunkTracker.addOrUpdate(player, newPos.x(), newPos.z(), ca.spottedleaf.moonrise.patches.chunk_tick_iteration.ChunkTickConstants.PLAYER_SPAWN_TRACK_RANGE);
            // Note: Cannot do narrow tracking on Paper due to custom spawn range
        }
    }

    @Override
    public final boolean moonrise$hasAnyNearbyNarrow(final int chunkX, final int chunkZ) {
        throw new UnsupportedOperationException(); // Note: Cannot do narrow tracking on Paper due to custom spawn range
    }

    @Inject(method = "runAllUpdates", at = @At("HEAD"), cancellable = true)
    private void felt$update(ChunkMap map, CallbackInfoReturnable<Boolean> cir) { if (this.felt$ready()) cir.setReturnValue(this.moonrise$getChunkHolderManager().processTicketUpdates()); }
    @Inject(method = "inEntityTickingRange", at = @At("HEAD"), cancellable = true)
    private void felt$entities(long key, CallbackInfoReturnable<Boolean> cir) { if (this.felt$ready()) { var h = this.moonrise$getChunkHolderManager().getChunkHolder(key); cir.setReturnValue(h != null && h.isEntityTickingReady()); } }
    @Inject(method = "inBlockTickingRange", at = @At("HEAD"), cancellable = true)
    private void felt$blocks(long key, CallbackInfoReturnable<Boolean> cir) { if (this.felt$ready()) { var h = this.moonrise$getChunkHolderManager().getChunkHolder(key); cir.setReturnValue(h != null && h.isTickingReady()); } }
    @Inject(method = "getChunkLevel", at = @At("HEAD"), cancellable = true)
    private void felt$level(long key, boolean simulation, CallbackInfoReturnable<Integer> cir) { if (this.felt$ready()) { var h = this.moonrise$getChunkHolderManager().getChunkHolder(key); cir.setReturnValue(h == null ? ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager.MAX_TICKET_LEVEL + 1 : h.getTicketLevel()); } }
    @Inject(method = "updateSimulationDistance", at = @At("HEAD"), cancellable = true)
    private void felt$simulation(int distance, CallbackInfo ci) { if (this.felt$ready()) { this.simulationDistance = net.minecraft.util.Mth.clamp(distance, 0, ca.spottedleaf.moonrise.common.util.MoonriseConstants.MAX_VIEW_DISTANCE); ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.moonrise$getChunkMap().level).moonrise$getPlayerChunkLoader().setTickDistance(this.simulationDistance); ci.cancel(); } }
    @Inject(method = "updatePlayerTickets", at = @At("HEAD"), cancellable = true)
    private void felt$view(int distance, CallbackInfo ci) { if (this.felt$ready()) ci.cancel(); }
    @Inject(method = "addPlayer", at = @At("RETURN"))
    private void felt$add(SectionPos pos, ServerPlayer player, CallbackInfo ci) { this.moonrise$addPlayer(player, pos); }
    @Inject(method = "removePlayer", at = @At("RETURN"))
    private void felt$remove(SectionPos pos, ServerPlayer player, CallbackInfo ci) { this.moonrise$removePlayer(player, pos); }
    @Inject(method = "getNaturalSpawnChunkCount", at = @At("HEAD"), cancellable = true)
    private void felt$count(CallbackInfoReturnable<Integer> cir) { if (this.felt$ready()) cir.setReturnValue(this.spawnChunkTracker.getTotalPositions()); }
    @Inject(method = "hasPlayersNearby", at = @At("HEAD"), cancellable = true)
    private void felt$near(long pos, CallbackInfoReturnable<net.minecraft.util.TriState> cir) { if (this.felt$ready()) cir.setReturnValue(this.spawnChunkTracker.hasObjectsNear(net.minecraft.world.level.ChunkPos.unpack(pos).x(), net.minecraft.world.level.ChunkPos.unpack(pos).z()) ? net.minecraft.util.TriState.DEFAULT : net.minecraft.util.TriState.FALSE); }
    @Inject(method = "getSpawnCandidateChunks", at = @At("HEAD"), cancellable = true)
    private void felt$candidates(CallbackInfoReturnable<it.unimi.dsi.fastutil.longs.LongIterator> cir) { if (this.felt$ready()) cir.setReturnValue(this.spawnChunkTracker.getPositions().iterator()); }
    @Inject(method = "forEachEntityTickingChunk", at = @At("HEAD"), cancellable = true)
    private void felt$iterate(it.unimi.dsi.fastutil.longs.LongConsumer consumer, CallbackInfo ci) { if (this.felt$ready()) {
        var chunks = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.moonrise$getChunkMap().level).moonrise$getEntityTickingChunks();
        for (int i = 0, size = chunks.size(); i < size; ++i) consumer.accept(chunks.getRawDataUnchecked()[i].getPos().pack());
        ci.cancel();
    } }

}
