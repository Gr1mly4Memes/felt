package gr1mly4memes.felt.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.server.level.ServerLevelBridge;
import gr1mly4memes.felt.bridge.core.world.level.block.RedstoneWireBridge;
import gr1mly4memes.felt.mixin.core.world.level.redstone.DefaultRedstoneWireEvaluatorAccessor;
import io.papermc.paper.configuration.WorldConfiguration.Misc.RedstoneImplementation;
import io.papermc.paper.redstone.RedstoneWireTurbo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireImplementationMixin implements RedstoneWireBridge {
    @Shadow @Final private RedstoneWireEvaluator evaluator;
    @Shadow private boolean shouldSignal;

    @Unique
    private final RedstoneWireTurbo felt$turbo = new RedstoneWireTurbo((RedstoneWireBlock) (Object) this);

    @WrapOperation(method = {"onPlace", "affectNeighborsAfterRemoval", "neighborChanged"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/RedstoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/redstone/Orientation;Z)V"),
        require = 3, allow = 3)
    private void felt$dispatchPowerUpdate(RedstoneWireBlock wire, Level level, BlockPos pos, BlockState state,
                                          Orientation orientation, boolean added, Operation<Void> original) {
        if (level instanceof ServerLevelBridge world && world.paperConfig() != null) {
            switch (world.paperConfig().misc.redstoneImplementation) {
                case EIGENCRAFT -> {
                    BlockPos source = orientation == null ? null : pos.relative(orientation.getFront().getOpposite());
                    this.felt$turbo.updateSurroundingRedstone(level, pos, state, source);
                    return;
                }
                case ALTERNATE_CURRENT -> {
                    if (added) {
                        world.bridge$getWireHandler().onWireAdded(pos, state);
                    } else {
                        world.bridge$getWireHandler().onWireRemoved(pos, state);
                    }
                    return;
                }
                case VANILLA -> {}
            }
        }
        original.call(wire, level, pos, state, orientation, added);
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void felt$handleAlternateCurrentUpdate(BlockState state, Level level, BlockPos pos, Block block,
                                                    Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
        if (level instanceof ServerLevelBridge world && world.paperConfig() != null
            && world.paperConfig().misc.redstoneImplementation == RedstoneImplementation.ALTERNATE_CURRENT) {
            world.bridge$getWireHandler().onWireUpdated(pos, state, orientation);
            ci.cancel();
        }
    }

    @Override
    public BlockState bridge$calculateCurrentChanges(Level level, BlockPos pos, BlockState state) {
        int oldPower = state.getValue(RedstoneWireBlock.POWER);
        int newPower = ((DefaultRedstoneWireEvaluatorAccessor) this.evaluator).felt$calculateTargetStrength(level, pos);
        if (oldPower != newPower && level.getBlockState(pos) == state) {
            newPower = CraftEventFactory.callRedstoneChange(level, pos, oldPower, newPower).getNewCurrent();
            state = state.setValue(RedstoneWireBlock.POWER, newPower);
            if (level.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS)) {
                this.felt$turbo.updateNeighborShapes(level, pos, state);
            }
        }
        return state;
    }

    @Override
    public boolean bridge$setSignalEnabled(boolean enabled) {
        boolean previous = this.shouldSignal;
        this.shouldSignal = enabled;
        return previous;
    }
}
