package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.*;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
@Mixin(ChunkMap.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseChunkMapMixin {
    @Shadow @Final public ServerLevel level;
    @Shadow public int serverViewDistance;
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager felt$manager() {
        var scheduler = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getChunkTaskScheduler();
        return scheduler == null ? null : scheduler.chunkHolderManager;
    }
    @Inject(method = "size", at = @At("HEAD"), cancellable = true)
    private void felt$count(CallbackInfoReturnable<Integer> cir) { if (this.felt$manager() != null) cir.setReturnValue(this.felt$manager().size()); }
    @Inject(method = "hasWork", at = @At("HEAD"), cancellable = true)
    private void felt$legacyWork(CallbackInfoReturnable<Boolean> cir) {
        // Vanilla's shutdown loop cannot drain native tickets. Native close awaits its own queues.
        if (this.felt$manager() != null) cir.setReturnValue(false);
    }

    @Inject(method = "collectSpawningChunks", at = @At("HEAD"), cancellable = true)
    private void felt$spawning(java.util.List<net.minecraft.world.level.chunk.LevelChunk> output, CallbackInfo ci) {
        if (this.felt$manager() == null) return;
        var chunks = ((ca.spottedleaf.moonrise.patches.chunk_tick_iteration.ChunkTickServerLevel)this.level).moonrise$getPlayerTickingChunks();
        for (int i = 0, size = chunks.size(); i < size; ++i) {
            var chunk = chunks.getRawDataUnchecked()[i];
            if (((ChunkMap)(Object)this).anyPlayerCloseEnoughForSpawning(chunk.getPos())) output.add(chunk);
        }
        ci.cancel();
    }
    @Inject(method = {"getUpdatingChunkIfPresent", "getVisibleChunkIfPresent"}, at = @At("HEAD"), cancellable = true)
    private void felt$get(long key, CallbackInfoReturnable<ChunkHolder> cir) { if (this.felt$manager() != null) { var h = this.felt$manager().getChunkHolder(key); cir.setReturnValue(h == null ? null : h.vanillaChunkHolder); } }
    @Inject(method = "saveAllChunks", at = @At("HEAD"), cancellable = true)
    private void felt$save(boolean flush, CallbackInfo ci) { if (this.felt$manager() != null) { this.felt$manager().saveAllChunks(flush, false, false, false); ci.cancel(); } }
    @Inject(method = "processUnloads", at = @At("HEAD"), cancellable = true)
    private void felt$unload(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) { if (this.felt$manager() != null) { this.felt$manager().processUnloads(); this.felt$manager().autoSave(); ci.cancel(); } }
    @Inject(method = "saveChunksEagerly", at = @At("HEAD"), cancellable = true)
    private void felt$noVanillaSave(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) { if (this.felt$manager() != null) ci.cancel(); }
    @Inject(method = "setChunkUnsaved", at = @At("HEAD"), cancellable = true)
    private void felt$noVanillaQueue(net.minecraft.world.level.ChunkPos pos, CallbackInfo ci) { if (this.felt$manager() != null) ci.cancel(); }
    @Inject(method = "runGenerationTasks", at = @At("HEAD"), cancellable = true)
    private void felt$noVanillaGeneration(CallbackInfo ci) { if (this.felt$manager() != null) ci.cancel(); }
    @Inject(method = "setServerViewDistance", at = @At("HEAD"), cancellable = true)
    private void felt$distance(int distance, CallbackInfo ci) { if (this.felt$manager() != null) {
        this.serverViewDistance = net.minecraft.util.Mth.clamp(distance, 2, ca.spottedleaf.moonrise.common.util.MoonriseConstants.MAX_VIEW_DISTANCE);
        ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getPlayerChunkLoader().setLoadDistance(this.serverViewDistance + 1);
        ci.cancel();
    } }
    @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
    private void felt$tracking(ServerPlayer player, CallbackInfo ci) { if (this.felt$manager() != null) ci.cancel(); }
    @Inject(method = "updatePlayerStatus", at = @At("RETURN"))
    private void felt$player(ServerPlayer player, boolean add, CallbackInfo ci) {
        var loader = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getPlayerChunkLoader();
        if (add) { loader.addPlayer(player); } else loader.removePlayer(player);
    }
    @Inject(method = "move", at = @At("RETURN"))
    private void felt$move(ServerPlayer player, CallbackInfo ci) { ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getPlayerChunkLoader().updatePlayer(player); }
    @Inject(method = "isChunkTracked", at = @At("HEAD"), cancellable = true)
    private void felt$sent(ServerPlayer player, int x, int z, CallbackInfoReturnable<Boolean> cir) { if (this.felt$manager() != null) cir.setReturnValue(((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getPlayerChunkLoader().isChunkSent(player, x, z)); }
    @Inject(method = "isChunkOnTrackedBorder", at = @At("HEAD"), cancellable = true)
    private void felt$edge(ServerPlayer player, int x, int z, CallbackInfoReturnable<Boolean> cir) { if (this.felt$manager() != null) cir.setReturnValue(((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.level).moonrise$getPlayerChunkLoader().isChunkSent(player, x, z, true)); }
    @Inject(method = "getPlayers", at = @At("HEAD"), cancellable = true)
    private void felt$watchers(net.minecraft.world.level.ChunkPos pos, boolean edge, CallbackInfoReturnable<java.util.List<ServerPlayer>> cir) { if (this.felt$manager() != null) { var holder = this.felt$manager().getChunkHolder(pos.pack()); cir.setReturnValue(holder == null ? java.util.List.of() : ((ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemChunkHolder)holder.vanillaChunkHolder).moonrise$getPlayers(edge)); } }

    public CompletableFuture<Optional<CompoundTag>> read(final ChunkPos pos) {
        final CompletableFuture<Optional<CompoundTag>> ret = new CompletableFuture<>();

        ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.loadDataAsync(
            this.level, pos.x(), pos.z(), ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionFileType.CHUNK_DATA,
            (final CompoundTag data, final Throwable thr) -> {
                if (thr != null) {
                    ret.completeExceptionally(thr);
                } else {
                    ret.complete(Optional.ofNullable(data));
                }
            }, false
        );

        return ret;
    }

    public CompletableFuture<Void> write(final ChunkPos pos, final java.util.function.Supplier<CompoundTag> tag) {
        ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.scheduleSave(
            this.level, pos.x(), pos.z(), tag.get(),
            ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionFileType.CHUNK_DATA
        );
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> synchronize(final boolean flush) {
        try {
            ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.flush(this.level);
            if (flush) {
                ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.flushRegionStorages(this.level);
            }
            return CompletableFuture.completedFuture(null);
        } catch (final Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}
