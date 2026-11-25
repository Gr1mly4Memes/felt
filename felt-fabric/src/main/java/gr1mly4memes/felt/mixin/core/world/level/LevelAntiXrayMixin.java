package gr1mly4memes.felt.mixin.core.world.level;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.server.level.ServerLevel.class)
public abstract class LevelAntiXrayMixin {
    @Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"))
    private void felt$revealChangedBlocks(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        ((WorldBridge) this).bridge$chunkPacketBlockController().onBlockChange(
            (Level) (Object) this, pos, newState, oldState, 0, 0);
    }
}
