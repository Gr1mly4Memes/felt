package gr1mly4memes.felt.mixin.core.world.level.redstone;

import gr1mly4memes.felt.bridge.core.server.level.ServerLevelBridge;
import io.papermc.paper.configuration.WorldConfiguration.Misc.RedstoneImplementation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ExperimentalRedstoneUtils.class)
public abstract class ExperimentalRedstoneUtilsMixin {
    @ModifyVariable(method = "initialOrientation", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Direction felt$defaultAlternateCurrentFront(Direction front, Level level) {
        if (front == null && level instanceof ServerLevelBridge world && world.paperConfig() != null
            && world.paperConfig().misc.redstoneImplementation == RedstoneImplementation.ALTERNATE_CURRENT) {
            return Direction.WEST;
        }
        return front;
    }
}
