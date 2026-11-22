package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.bridge.core.world.level.block.BlockDestructionBridge;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory.BlockDestruction;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Block.class)
public abstract class BlockDestructionDropsMixin implements BlockDestructionBridge {

    @Decorate(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;"), require = 1, allow = 1)
    private static List<ItemStack> felt$editBlockBreakDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity) throws Throwable {
        List<ItemStack> drops = (List<ItemStack>) DecorationOps.callsite().invoke(state, level, pos, blockEntity);
        BlockDestruction context = BlockDestruction.current(level, pos);
        if (context != null && context.isBlockBreak()) {
            return context.editVanillaDrops(state, level, pos, drops);
        }
        return drops;
    }
}
