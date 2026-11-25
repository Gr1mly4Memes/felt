package gr1mly4memes.felt.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SpreadingSnowyBlock.class)
public abstract class SpreadingSnowyDirtBlockMixin extends SnowyBlock {
    @Shadow @Final private ResourceKey<Block> baseBlock;

    protected SpreadingSnowyDirtBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * @author Felt
     * @reason Reuse loaded chunk data for Paper's fade and spread checks and dispatch the corresponding Bukkit events.
     */
    @Overwrite
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        Block base = world.registryAccess().lookupOrThrow(Registries.BLOCK).getOptional(this.baseBlock).orElse(null);
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        var chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (base == null || chunk == null) {
            return;
        }
        BlockPos above = pos.above();
        if (!felt$canStayAlive(state, chunk.getBlockState(above))) {
            BlockState replacement = base.defaultBlockState();
            if (!CraftEventFactory.callBlockFadeEvent(world, pos, replacement).isCancelled()) {
                world.setBlockAndUpdate(pos, replacement);
            }
            return;
        }
        if (world.getMaxLocalRawBrightness(above) < 9) {
            return;
        }

        BlockState spread = this.defaultBlockState();
        for (int attempt = 0; attempt < 4; attempt++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (pos.equals(target)) {
                continue;
            }
            var targetChunk = (target.getX() >> 4) == chunkX && (target.getZ() >> 4) == chunkZ ? chunk : world.getChunkAt(target);
            if (!targetChunk.getBlockState(target).is(base)) {
                continue;
            }
            BlockState covering = targetChunk.getBlockState(target.above());
            if (felt$canStayAlive(spread, covering) && !covering.getFluidState().is(FluidTags.WATER)) {
                CraftEventFactory.handleBlockSpreadEvent(world, pos, target,
                    spread.setValue(SNOWY, isSnowySetting(covering)), Block.UPDATE_ALL);
            }
        }
    }

    @Unique
    private static boolean felt$canStayAlive(BlockState state, BlockState covering) {
        if (covering.is(Blocks.SNOW) && covering.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        return !covering.getFluidState().isFull()
            && LightEngine.getLightDampeningInto(state, covering, Direction.UP, covering.getLightDampening()) < 15;
    }
}
