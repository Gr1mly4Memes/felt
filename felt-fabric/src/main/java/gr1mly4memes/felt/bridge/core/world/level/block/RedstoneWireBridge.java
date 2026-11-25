package gr1mly4memes.felt.bridge.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface RedstoneWireBridge {
    BlockState bridge$calculateCurrentChanges(Level level, BlockPos pos, BlockState state);

    boolean bridge$setSignalEnabled(boolean enabled);
}
