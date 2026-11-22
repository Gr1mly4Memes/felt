package gr1mly4memes.felt.mixin.core.server.commands;

import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetSpawnCommand;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(SetSpawnCommand.class)
public class SetSpawnCommandMixin {

    @Inject(method = "setSpawn", at = @At("HEAD"))
    private static void felt$cause(CommandSourceStack p_138650_, Collection<ServerPlayer> players, BlockPos p_138652_, Coordinates p_138653_, CallbackInfoReturnable<Integer> cir) {
        for (ServerPlayer player : players) {
            ((ServerPlayerBridge) player).bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.COMMAND);
        }
    }
}
