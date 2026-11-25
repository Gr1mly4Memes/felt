package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeAntiXrayMixin {
    @Shadow protected ServerLevel level;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void felt$revealLeftClickedBlocks(BlockPos pos, ServerboundPlayerActionPacket.Action action,
            Direction direction, int height, int sequence, CallbackInfo ci) {
        ((WorldBridge) this.level).bridge$chunkPacketBlockController().onPlayerLeftClickBlock(
            this.level, pos, action, direction, height, sequence);
    }
}
