package gr1mly4memes.felt.mixin.core.world.level;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import gr1mly4memes.felt.bridge.core.world.level.block.BlockDestructionBridge;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory.BlockDestruction;
import gr1mly4memes.felt.mod.util.DistValidate;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelBlockDestroyEventMixin {

    @Decorate(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"), require = 1, allow = 1)
    private void felt$blockDestroy(BlockPos pos, boolean dropResources, Entity breaker, int updateLimit,
                                       @Local(ordinal = 0) BlockState state,
                                       @Local(allocate = "feltBlockDestroyEvent") BlockDestroyEvent event,
                                       @Local(allocate = "feltBlockDestroyExperience") int experience) throws Throwable {
        FluidState fluid = ((Level) (Object) this).getFluidState(pos);
        event = null;
        experience = 0;
        if ((Object) this instanceof ServerLevel level && DistValidate.isValid(level)) {
            experience = ((BlockDestructionBridge) state.getBlock()).bridge$getDestructionExperience(state, level, pos, breaker);
            if (BlockDestroyEvent.getHandlerList().getRegisteredListeners().length > 0) {
                event = new BlockDestroyEvent(CraftBlock.at(level, pos), CraftBlockData.createData(fluid.createLegacyBlock()),
                    CraftBlockData.createData(state), experience, dropResources);
                if (!event.callEvent()) {
                    DecorationOps.cancel().invoke(false);
                    return;
                }
                dropResources = event.willDrop();
                experience = event.getExpToDrop();
            }
        }
        DecorationOps.blackhole().invoke(dropResources, event, experience);
    }

    @Decorate(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;spawnDestroyParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"), require = 1, allow = 1)
    private void felt$blockDestroyEffect(Block block, Level level, BlockPos pos, BlockState effectState,
                                             @Local(allocate = "feltBlockDestroyEvent") BlockDestroyEvent event) throws Throwable {
        if (event != null) {
            if (!event.playEffect()) {
                return;
            }
            effectState = ((CraftBlockData) event.getEffectBlock()).getState();
            block = effectState.getBlock();
        }
        DecorationOps.callsite().invoke(block, level, pos, effectState);
    }

    @Decorate(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"), require = 1, allow = 1)
    private void felt$blockDestroyDrops(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity,
                                            Entity breaker, ItemStack tool,
                                            @Local(allocate = "feltBlockDestroyExperience") int experience) throws Throwable {
        var previous = gr1mly4memes.felt.mod.util.FeltCaptures.blockDestruction;
        BlockDestruction context = level instanceof ServerLevel serverLevel && DistValidate.isValid(level)
            ? BlockDestruction.blockDestroy(serverLevel, pos, experience) : null;
        if (context != null) gr1mly4memes.felt.mod.util.FeltCaptures.blockDestruction = context;
        try {
            DecorationOps.callsite().invoke(state, level, pos, blockEntity, breaker, tool);
        } finally {
            if (context != null) {
                gr1mly4memes.felt.mod.util.FeltCaptures.blockDestruction = previous;
            }
        }
    }
}
