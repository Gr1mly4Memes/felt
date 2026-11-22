package gr1mly4memes.felt.bridge.core.world.level.material;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface LavaFluidBridge {

    default BlockState bridge$mod$fireFluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
        return state;
    }

    boolean bridge$mod$isFlammable(LevelReader level, BlockPos pos, Direction face);
}
