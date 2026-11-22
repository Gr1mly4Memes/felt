package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.bridge.core.server.level.ChunkMapBridge;
import gr1mly4memes.felt.bridge.core.world.server.ChunkHolderBridge;
import gr1mly4memes.felt.bridge.core.world.level.chunk.LevelChunkBridge;
import gr1mly4memes.felt.mod.server.FeltServer;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class ChunkHolderLoadCallbackMixin {
    @Shadow public int oldTicketLevel;
    @Shadow private int ticketLevel;
    @Shadow public abstract CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture();

    // Note that this logic is slightly different from the one above
    @Inject(method = "updateFutures", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ChunkHolder$LevelChangeListener;onLevelChange(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/IntSupplier;ILjava/util/function/IntConsumer;)V"))
    private void felt$onChunkLoad(ChunkMap chunkManager, Executor executor, CallbackInfo ci) {
        ((ChunkHolderBridge) this).bridge$resetPendingUnload();
        FullChunkStatus fullChunkStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus fullChunkStatus2 = ChunkLevel.fullStatus(this.ticketLevel);
        this.oldTicketLevel = this.ticketLevel;
        if (!fullChunkStatus.isOrAfter(FullChunkStatus.FULL) && fullChunkStatus2.isOrAfter(FullChunkStatus.FULL)) {
            this.getFullChunkFuture().thenAccept((either) -> {
                LevelChunk chunk = either.orElse(null);
                if (chunk != null) {
                    ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().execute(
                            ((LevelChunkBridge) chunk)::bridge$loadCallback
                    );
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                FeltServer.LOGGER.fatal("Failed to schedule load callback for chunk " + ((ChunkHolder) (Object) this).getPos(), throwable);
                return null;
            });

            ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }
}
