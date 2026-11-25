package gr1mly4memes.felt.mixin.core.world.level.block.entity.trialspawner;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.mod.server.world.FeltSpawningInfluence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerDetector.class)
public interface TrialPlayerSpawningInfluenceMixin {
    @ModifyExpressionValue(method = "lambda$static$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z"))
    private static boolean felt$trialSpawnInfluence(boolean spectator, @Local(argsOnly = true) Player player) {
        return spectator || !FeltSpawningInfluence.affects(player);
    }
}
