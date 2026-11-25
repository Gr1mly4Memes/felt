package gr1mly4memes.felt.mixin.core.world.level.chunk;

import ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk;
import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ImposterProtoChunk.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseImposterLightMixin implements StarlightChunk {
    @Shadow @Final private LevelChunk wrapped;
    @Override public SWMRNibbleArray[] starlight$getBlockNibbles() { return ((StarlightChunk) this.wrapped).starlight$getBlockNibbles(); }
    @Override public void starlight$setBlockNibbles(SWMRNibbleArray[] value) { ((StarlightChunk) this.wrapped).starlight$setBlockNibbles(value); }
    @Override public SWMRNibbleArray[] starlight$getSkyNibbles() { return ((StarlightChunk) this.wrapped).starlight$getSkyNibbles(); }
    @Override public void starlight$setSkyNibbles(SWMRNibbleArray[] value) { ((StarlightChunk) this.wrapped).starlight$setSkyNibbles(value); }
    @Override public boolean[] starlight$getSkyEmptinessMap() { return ((StarlightChunk) this.wrapped).starlight$getSkyEmptinessMap(); }
    @Override public void starlight$setSkyEmptinessMap(boolean[] value) { ((StarlightChunk) this.wrapped).starlight$setSkyEmptinessMap(value); }
    @Override public boolean[] starlight$getBlockEmptinessMap() { return ((StarlightChunk) this.wrapped).starlight$getBlockEmptinessMap(); }
    @Override public void starlight$setBlockEmptinessMap(boolean[] value) { ((StarlightChunk) this.wrapped).starlight$setBlockEmptinessMap(value); }
}
