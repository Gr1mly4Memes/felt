package gr1mly4memes.felt.mixin.core.world.level.levelgen;

import gr1mly4memes.felt.bridge.core.world.level.levelgen.WorldGenerationContextBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(WorldGenerationContext.class)
public class WorldGenerationContextMixin implements WorldGenerationContextBridge {

    @Unique
    private Level felt$level;

    @Override
    public Level bridge$getLevel() {
        return Objects.requireNonNull(this.felt$level, "WorldGenerationContext has no level");
    }

    @Override
    public void bridge$setLevel(Level level) {
        this.felt$level = level;
    }
}
