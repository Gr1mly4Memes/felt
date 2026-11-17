package gr1mly4memes.felt.common.mixin.core.world.level.block.piston;

import gr1mly4memes.felt.common.mod.server.event.FeltEventFactory.BlockDestruction;
import gr1mly4memes.felt.common.mod.util.DistValidate;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBlockBreakEventMixin {

    @Decorate(method = "moveBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"), require = 1, allow = 1)
    private void arclight$blockBreakSource(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity blockEntity,
                                         Level hostLevel, BlockPos pistonPos, Direction direction, boolean extending) throws Throwable {
        var previous = gr1mly4memes.felt.common.mod.util.FeltCaptures.blockDestruction;
        BlockDestruction context = DistValidate.isValid(level)
            ? BlockDestruction.blockBreak(level, pos, pistonPos) : null;
        if (context != null) gr1mly4memes.felt.common.mod.util.FeltCaptures.blockDestruction = context;
        try {
            DecorationOps.callsite().invoke(state, level, pos, blockEntity);
        } finally {
            if (context != null) {
                gr1mly4memes.felt.common.mod.util.FeltCaptures.blockDestruction = previous;
            }
        }
    }
}
