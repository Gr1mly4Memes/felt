package gr1mly4memes.felt.common.mixin.core.world.level.material;

import gr1mly4memes.felt.common.mod.server.event.FeltEventFactory.BlockDestruction;
import gr1mly4memes.felt.common.mod.util.DistValidate;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidBlockBreakEventMixin {

    @Decorate(method = "spreadTo(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;beforeDestroyingBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"), require = 1, allow = 1)
    private void arclight$blockBreakSource(FlowingFluid fluid, LevelAccessor level, BlockPos pos, BlockState state,
                                         LevelAccessor hostLevel, BlockPos hostPos, BlockState hostState,
                                         Direction direction, FluidState target) throws Throwable {
        var previous = gr1mly4memes.felt.common.mod.util.FeltCaptures.blockDestruction;
        BlockDestruction context = fluid instanceof WaterFluid && DistValidate.isValid(level)
            ? BlockDestruction.blockBreak(level, pos, pos.relative(direction.getOpposite())) : null;
        if (context != null) gr1mly4memes.felt.common.mod.util.FeltCaptures.blockDestruction = context;
        try {
            DecorationOps.callsite().invoke(fluid, level, pos, state);
        } finally {
            if (context != null) {
                gr1mly4memes.felt.common.mod.util.FeltCaptures.blockDestruction = previous;
            }
        }
    }
}
