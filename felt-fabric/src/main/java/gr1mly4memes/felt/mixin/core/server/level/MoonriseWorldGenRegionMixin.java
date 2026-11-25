package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.core.BlockPos;
@Mixin(net.minecraft.server.level.WorldGenRegion.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseWorldGenRegionMixin {
    @Shadow public abstract net.minecraft.world.level.chunk.ChunkAccess getChunk(int x, int z);
    @Shadow public abstract net.minecraft.world.level.lighting.LevelLightEngine getLightEngine();
    public int getBrightness(final net.minecraft.world.level.LightLayer lightLayer, final BlockPos blockPos) {
        final ChunkAccess chunk = this.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        if (!chunk.isLightCorrect()) {
            return 0;
        }
        return this.getLightEngine().getLayerListener(lightLayer).getLightValue(blockPos);
    }

    /**
     * See above
     */
    public int getRawBrightness(final BlockPos blockPos, final int subtract) {
        final ChunkAccess chunk = this.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        if (!chunk.isLightCorrect()) {
            return 0;
        }
        return this.getLightEngine().getRawBrightness(blockPos, subtract);
    }

}
