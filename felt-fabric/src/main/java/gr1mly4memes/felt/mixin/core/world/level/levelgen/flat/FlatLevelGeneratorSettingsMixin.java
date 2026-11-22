package gr1mly4memes.felt.mixin.core.world.level.levelgen.flat;

import gr1mly4memes.felt.bridge.core.world.level.levelgen.flat.FlatLevelGeneratorSettingsBridge;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FlatLevelGeneratorSettings.class)
public class FlatLevelGeneratorSettingsMixin implements FlatLevelGeneratorSettingsBridge {

    @Unique
    private BiomeSource felt$biomeSource;

    @Override
    public void bridge$setBiomeSource(BiomeSource biomeSource) {
        felt$biomeSource = biomeSource;
    }

    @Override
    public FlatLevelGeneratorSettings bridge$withBiomeSource(BiomeSource biomeSource) {
        felt$biomeSource = biomeSource;
        return (FlatLevelGeneratorSettings) (Object) this;
    }

    @Override
    public BiomeSource bridge$getBiomeSource() {
        return felt$biomeSource;
    }
}
