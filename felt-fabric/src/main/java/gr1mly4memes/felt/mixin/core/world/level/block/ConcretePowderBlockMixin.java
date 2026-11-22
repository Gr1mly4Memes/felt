package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.bridge.core.world.level.LevelAccessorBridge;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.event.block.BlockFormEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConcretePowderBlock.class)
public abstract class ConcretePowderBlockMixin extends FallingBlockMixin {

    // @formatter:off
    @Shadow @Final private Block concrete;
    // @formatter:on

    @Decorate(method = "onLand", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean felt$blockForm(Level world, BlockPos pos, BlockState newState) throws Throwable {
        if (LevelAccessorBridge.from(world) instanceof LevelAccessorBridge bridge) {
            final var event = FeltEventFactory.callBlockFormEvent(bridge.bridge$getMinecraftWorld(), pos, newState, Block.UPDATE_ALL, null);
            if (event != null) {
                if (event.isCancelled()) {
                    return false;
                }
                newState = ((CraftBlockState) event.getNewState()).getHandle();
            }
        }
        return (boolean) DecorationOps.callsite().invoke(world, pos, newState);
    }

    @Redirect(method = "getStateForPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState felt$blockForm(Block instance, BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        CraftBlockState blockState = CraftBlockStates.getBlockState(world, blockPos);
        blockState.setBlock(this.concrete.defaultBlockState());
        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return blockState.getHandle();
        }
        return super.getStateForPlacement(context);
    }

    @Redirect(method = "updateShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState felt$blockForm(Block instance, BlockState state, LevelReader level,
                                         ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction,
                                         BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (!(level instanceof Level world)) {
            return this.concrete.defaultBlockState();
        }
        CraftBlockState blockState = CraftBlockStates.getBlockState(world, pos);
        blockState.setBlock(this.concrete.defaultBlockState());
        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return blockState.getHandle();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }
}
