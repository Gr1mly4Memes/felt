package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Eject;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.SculkVeinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkVeinBlock.class)
public class SculkVeinBlockMixin {

    @Decorate(method = "attemptUseCharge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SculkVeinBlock;attemptPlaceSculk(Lnet/minecraft/world/level/block/SculkSpreader;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z"))
    private boolean felt$captureSpreadPos(SculkVeinBlock instance, SculkSpreader spreader, LevelAccessor level, BlockPos pos, RandomSource random, @Local(ordinal = 0) BlockPos originPos) throws Throwable {
        BlockPos source = originPos.immutable();
        BlockPos old = FeltCaptures.captureSpreadSource(source);
        try {
            return (boolean) DecorationOps.callsite().invoke(instance, spreader, level, pos, random);
        } finally {
            FeltCaptures.resetSpreadSource(old, source);
        }
    }

    @Eject(method = "attemptPlaceSculk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean felt$blockSpread(LevelAccessor level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!CraftEventFactory.handleBlockSpreadEvent(level, FeltCaptures.getSpreadPos(), pos, state, Block.UPDATE_ALL)) {
            cir.setReturnValue(false);
            return false;
        }
        return true;
    }
}
