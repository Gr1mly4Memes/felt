package gr1mly4memes.felt.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.mod.server.world.FeltSpawningInfluence;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Silverfish.class)
public abstract class SilverfishSpawningInfluenceMixin {
    @WrapOperation(method = "checkSilverfishSpawnRules", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;getNearestPlayer(DDDDZ)Lnet/minecraft/world/entity/player/Player;"), require = 1, allow = 1)
    private static Player felt$influencingPlayer(LevelAccessor level, double x, double y, double z, double range, boolean excludeCreative, Operation<Player> original) {
        return FeltSpawningInfluence.nearest(level, x, y, z, range, excludeCreative);
    }
}
