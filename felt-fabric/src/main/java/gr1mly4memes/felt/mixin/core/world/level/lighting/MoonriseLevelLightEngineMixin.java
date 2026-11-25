package gr1mly4memes.felt.mixin.core.world.level.lighting;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.lighting.*;
import org.jspecify.annotations.Nullable;

@Mixin(LevelLightEngine.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseLevelLightEngineMixin implements ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider {
    @Unique private ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface lightEngine;
    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LightChunkGetter;ZZ)V", at = @At("RETURN"))
    private void felt$init(LightChunkGetter getter, boolean block, boolean sky, CallbackInfo ci) {
        this.lightEngine = new ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface(
            getter.getLevel() instanceof net.minecraft.world.level.Level ? getter : null, sky, block, (LevelLightEngine)(Object)this);
    }
    @Inject(method = "<init>()V", at = @At("RETURN"))
    private void felt$empty(CallbackInfo ci) {
        this.lightEngine = new ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface(null, false, false, (LevelLightEngine)(Object)this);
    }
    @Override
    public final ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface starlight$getLightEngine() {
        return this.lightEngine;
    }

    @Override
    public void starlight$clientUpdateLight(final LightLayer lightType, final SectionPos pos,
                                            final DataLayer nibble, final boolean trustEdges) {
        throw new IllegalStateException("This hook is for the CLIENT ONLY"); // Paper - not implemented on server
    }

    @Override
    public void starlight$clientRemoveLightData(final ChunkPos chunkPos) {
        throw new IllegalStateException("This hook is for the CLIENT ONLY"); // Paper - not implemented on server
    }

    @Override
    public void starlight$clientChunkLoad(final ChunkPos pos, final net.minecraft.world.level.chunk.LevelChunk chunk) {
        throw new IllegalStateException("This hook is for the CLIENT ONLY"); // Paper - not implemented on server
    }
    // Paper end - rewrite chunk system

    @Overwrite
    public void checkBlock(final BlockPos pos) {
        this.lightEngine.blockChange(pos.immutable()); // Paper - rewrite chunk system
    }

    @Overwrite
    public boolean hasLightWork() {
        return this.lightEngine.hasUpdates(); // Paper - rewrite chunk system
    }

    @Overwrite
    public int runLightUpdates() {
        // Paper start - rewrite chunk system
        final boolean hadUpdates = this.hasLightWork();
        this.lightEngine.propagateChanges();
        return hadUpdates ? 1 : 0;
        // Paper end - rewrite chunk system
    }

    @Overwrite
    public void updateSectionStatus(final SectionPos pos, final boolean sectionEmpty) {
        this.lightEngine.sectionChange(pos, sectionEmpty); // Paper - rewrite chunk system
    }

    @Overwrite
    public void setLightEnabled(final ChunkPos pos, final boolean enable) {
        // Paper - rewrite chunk system
    }

    @Overwrite
    public void propagateLightSources(final ChunkPos pos) {
        // Paper - rewrite chunk system
    }

    @Overwrite
    public LayerLightEventListener getLayerListener(final LightLayer layer) {
        return layer == LightLayer.BLOCK ? this.lightEngine.getBlockReader() : this.lightEngine.getSkyReader(); // Paper - rewrite chunk system
    }

    @Overwrite
    public String getDebugData(final LightLayer layer, final SectionPos pos) {
        return "n/a"; // Paper - rewrite chunk system
    }

    @Overwrite
    public LayerLightSectionStorage.SectionType getDebugSectionType(final LightLayer layer, final SectionPos pos) {
        throw new UnsupportedOperationException(); // Paper - rewrite chunk system
    }

    @Overwrite
    public void queueSectionData(final LightLayer layer, final SectionPos pos, final @Nullable DataLayer data) {
        // Paper - rewrite chunk system
    }

    @Overwrite
    public void retainData(final ChunkPos pos, final boolean retain) {
        // Paper - rewrite chunk system
    }

    @Overwrite
    public int getRawBrightness(final BlockPos pos, final int skyDampen) {
        return this.lightEngine.getRawBrightness(pos, skyDampen); // Paper - rewrite chunk system
    }

    @Overwrite
    public boolean lightOnInColumn(final long sectionZeroNode) {
        throw new UnsupportedOperationException(); // Paper - rewrite chunk system // Paper - not implemented on server
    }


}
