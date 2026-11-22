package gr1mly4memes.felt.bridge.core.world.level.chunk;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public interface LevelChunkSectionBridge {

    void bridge$setBiome(int x, int y, int z, Holder<Biome> biome);

    default void setNoiseBiome(int x, int y, int z, Holder<Biome> biome) {
        this.bridge$setBiome(x, y, z, biome);
    }

}
