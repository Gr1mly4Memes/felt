package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
@Mixin(GenerationChunkHolder.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseGenerationHolderMixin {
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder felt$holder() {
        return (Object)this instanceof ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemChunkHolder holder ? holder.moonrise$getRealChunkHolder() : null;
    }
    @Inject(method = "getChunkIfPresentUnchecked", at = @At("HEAD"), cancellable = true)
    private void felt$getChunkIfPresentUnchecked(ChunkStatus status, CallbackInfoReturnable<ChunkAccess> cir) { if (this.felt$holder() != null) cir.setReturnValue(this.felt$holder().getChunkIfPresentUnchecked(status)); }
    @Inject(method = "getChunkIfPresent", at = @At("HEAD"), cancellable = true)
    private void felt$getChunkIfPresent(ChunkStatus status, CallbackInfoReturnable<ChunkAccess> cir) { if (this.felt$holder() != null) cir.setReturnValue(this.felt$holder().getChunkIfPresent(status)); }
    @Inject(method = "getLatestChunk", at = @At("HEAD"), cancellable = true)
    private void felt$getLatestChunk(CallbackInfoReturnable<ChunkAccess> cir) { if (this.felt$holder() != null) cir.setReturnValue(this.felt$holder().getLastChunkCompletion() == null ? null : this.felt$holder().getLastChunkCompletion().chunk()); }
    @Inject(method = "getLatestStatus", at = @At("HEAD"), cancellable = true)
    private void felt$getLatestStatus(CallbackInfoReturnable<ChunkStatus> cir) { if (this.felt$holder() != null) cir.setReturnValue(this.felt$holder().getLastChunkCompletion() == null ? null : this.felt$holder().getLastChunkCompletion().genStatus()); }
    @Inject(method = "getFullStatus", at = @At("HEAD"), cancellable = true)
    private void felt$getFullStatus(CallbackInfoReturnable<FullChunkStatus> cir) { if (this.felt$holder() != null) cir.setReturnValue(this.felt$holder().getChunkStatus()); }

}
