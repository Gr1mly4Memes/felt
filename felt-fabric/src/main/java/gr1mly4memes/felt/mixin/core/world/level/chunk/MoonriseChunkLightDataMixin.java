package gr1mly4memes.felt.mixin.core.world.level.chunk;

import ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk;
import ca.spottedleaf.moonrise.patches.starlight.light.StarLightEngine;
import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkAccess.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseChunkLightDataMixin implements StarlightChunk {
    @Unique private volatile SWMRNibbleArray[] felt$blockNibbles, felt$skyNibbles;
    @Unique private volatile boolean[] felt$skyEmptiness, felt$blockEmptiness;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$initializeLight(CallbackInfo ci) {
        if (!((Object) this instanceof ImposterProtoChunk)) {
            this.felt$blockNibbles = StarLightEngine.getFilledEmptyLight((ChunkAccess) (Object) this);
            this.felt$skyNibbles = StarLightEngine.getFilledEmptyLight((ChunkAccess) (Object) this);
        }
    }
    @Override public SWMRNibbleArray[] starlight$getBlockNibbles() { return this.felt$blockNibbles; }
    @Override public void starlight$setBlockNibbles(SWMRNibbleArray[] value) { this.felt$blockNibbles = value; }
    @Override public SWMRNibbleArray[] starlight$getSkyNibbles() { return this.felt$skyNibbles; }
    @Override public void starlight$setSkyNibbles(SWMRNibbleArray[] value) { this.felt$skyNibbles = value; }
    @Override public boolean[] starlight$getSkyEmptinessMap() { return this.felt$skyEmptiness; }
    @Override public void starlight$setSkyEmptinessMap(boolean[] value) { this.felt$skyEmptiness = value; }
    @Override public boolean[] starlight$getBlockEmptinessMap() { return this.felt$blockEmptiness; }
    @Override public void starlight$setBlockEmptinessMap(boolean[] value) { this.felt$blockEmptiness = value; }
}
