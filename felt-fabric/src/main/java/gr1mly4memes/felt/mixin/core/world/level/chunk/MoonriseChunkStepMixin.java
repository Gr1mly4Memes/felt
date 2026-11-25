package gr1mly4memes.felt.mixin.core.world.level.chunk;

import ca.spottedleaf.moonrise.patches.chunk_system.status.ChunkSystemChunkStep;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.world.level.chunk.status.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkStep.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseChunkStepMixin implements ChunkSystemChunkStep {
    @Shadow public abstract int getAccumulatedRadiusOf(ChunkStatus status);
    @Unique private ChunkStatus[] felt$byRadius;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$dependencies(ChunkStatus target, ChunkDependencies direct, ChunkDependencies accumulated,
                                   int radius, ChunkStatusTask task, CallbackInfo ci) {
        this.felt$byRadius = new ChunkStatus[this.getAccumulatedRadiusOf(ChunkStatus.EMPTY) + 1];
        this.felt$byRadius[0] = target.getParent();
        for (ChunkStatus status = target.getParent(); status != ChunkStatus.EMPTY; status = status.getParent()) {
            for (int i = 0, max = this.getAccumulatedRadiusOf(status); i <= max; i++) {
                if (this.felt$byRadius[i] == null) this.felt$byRadius[i] = status;
            }
        }
    }
    @Override public ChunkStatus moonrise$getRequiredStatusAtRadius(int radius) { return this.felt$byRadius[radius]; }
}
