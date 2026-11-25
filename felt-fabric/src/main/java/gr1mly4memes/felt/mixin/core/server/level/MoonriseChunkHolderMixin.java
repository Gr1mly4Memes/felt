package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/** Native Paper chunk system binding; the external Moonrise mod owns its own bindings. */
@Mixin(net.minecraft.server.level.ChunkHolder.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseChunkHolderMixin implements ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemChunkHolder {
    @Shadow @Final private ChunkHolder.PlayerProvider playerProvider;
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder newChunkHolder;

    @Unique private static final ServerPlayer[] EMPTY_PLAYER_ARRAY = new ServerPlayer[0];
    @Unique private final ca.spottedleaf.moonrise.common.list.ReferenceList<ServerPlayer> playersSentChunkTo = new ca.spottedleaf.moonrise.common.list.ReferenceList<>(EMPTY_PLAYER_ARRAY);

    @Unique private ChunkMap getChunkMap() {
        return (ChunkMap)this.playerProvider;
    }

    @Override
    public final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder moonrise$getRealChunkHolder() {
        return this.newChunkHolder;
    }

    @Override
    public final void moonrise$setRealChunkHolder(final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder newChunkHolder) {
        this.newChunkHolder = newChunkHolder;
    }

    @Override
    public final void moonrise$addReceivedChunk(final ServerPlayer player) {
        if (!this.playersSentChunkTo.add(player)) {
            throw new IllegalStateException("Already sent chunk " + ((ChunkHolder)(Object)this).getPos() + " in world '" + ca.spottedleaf.moonrise.common.util.WorldUtil.getWorldName(this.getChunkMap().level) + "' to player " + player);
        }
    }

    @Override
    public final void moonrise$removeReceivedChunk(final ServerPlayer player) {
        if (!this.playersSentChunkTo.remove(player)) {
            throw new IllegalStateException("Already sent chunk " + ((ChunkHolder)(Object)this).getPos() + " in world '" + ca.spottedleaf.moonrise.common.util.WorldUtil.getWorldName(this.getChunkMap().level) + "' to player " + player);
        }
    }

    @Override
    public final boolean moonrise$hasChunkBeenSent() {
        return this.playersSentChunkTo.size() != 0;
    }

    @Override
    public final boolean moonrise$hasChunkBeenSent(final ServerPlayer to) {
        return this.playersSentChunkTo.contains(to);
    }

    @Override
    public final List<ServerPlayer> moonrise$getPlayers(final boolean onlyOnWatchDistanceEdge) {
        final List<ServerPlayer> ret = new java.util.ArrayList<>();
        final ServerPlayer[] raw = this.playersSentChunkTo.getRawDataUnchecked();
        for (int i = 0, len = this.playersSentChunkTo.size(); i < len; ++i) {
            final ServerPlayer player = raw[i];
            if (onlyOnWatchDistanceEdge && !((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.getChunkMap().level).moonrise$getPlayerChunkLoader().isChunkSent(player, ((ChunkHolder)(Object)this).getPos().x(), ((ChunkHolder)(Object)this).getPos().z(), onlyOnWatchDistanceEdge)) {
                continue;
            }
            ret.add(player);
        }

        return ret;
    }

    @Override
    public final LevelChunk moonrise$getFullChunk() {
        if (this.newChunkHolder.isFullChunkReady()) {
            if (this.newChunkHolder.getCurrentChunk() instanceof LevelChunk levelChunk) {
                return levelChunk;
            } // else: race condition: chunk unload
        }
        return null;
    }

    @Unique private boolean isRadiusLoaded(final int radius) {
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager manager = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.getChunkMap().level).moonrise$getChunkTaskScheduler()
            .chunkHolderManager;
        final ChunkPos pos = ((ChunkHolder)(Object)this).getPos();
        final int chunkX = pos.x();
        final int chunkZ = pos.z();
        for (int dz = -radius; dz <= radius; ++dz) {
            for (int dx = -radius; dx <= radius; ++dx) {
                if ((dx | dz) == 0) {
                    continue;
                }

                final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder holder = manager.getChunkHolder(dx + chunkX, dz + chunkZ);

                if (holder == null || !holder.isFullChunkReady()) {
                    return false;
                }
            }
        }

        return true;
    }


    @Inject(method = "getTicketLevel", at = @At("HEAD"), cancellable = true)
    private void felt$level(CallbackInfoReturnable<Integer> cir) { if (this.newChunkHolder != null) cir.setReturnValue(this.newChunkHolder.getTicketLevel()); }
    @Inject(method = "getTickingChunk", at = @At("HEAD"), cancellable = true)
    private void felt$ticking(CallbackInfoReturnable<LevelChunk> cir) { if (this.newChunkHolder != null) cir.setReturnValue(this.newChunkHolder.isTickingReady() ? (LevelChunk)this.newChunkHolder.getCurrentChunk() : null); }
    @Inject(method = "getChunkToSend", at = @At("HEAD"), cancellable = true)
    private void felt$send(CallbackInfoReturnable<LevelChunk> cir) { if (this.newChunkHolder != null) cir.setReturnValue(this.isRadiusLoaded(1) ? this.moonrise$getFullChunk() : null); }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = {"blockChanged", "sectionLightChanged"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;getTickingChunk()Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk felt$broadcastChunk(ChunkHolder holder, com.llamalad7.mixinextras.injector.wrapoperation.Operation<LevelChunk> original) {
        if (this.newChunkHolder == null) return original.call(holder);
        return this.moonrise$hasChunkBeenSent() ? holder.getChunkToSend() : null;
    }
}
