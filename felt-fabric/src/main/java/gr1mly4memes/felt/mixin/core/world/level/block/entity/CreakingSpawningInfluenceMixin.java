package gr1mly4memes.felt.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.mod.server.world.FeltSpawningInfluence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CreakingHeartBlockEntity.class)
public abstract class CreakingSpawningInfluenceMixin {
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getNearestPlayer(DDDDZ)Lnet/minecraft/world/entity/player/Player;"))
    private static Player felt$creakingSpawnPlayer(Level level, double x, double y, double z, double range, boolean excludeCreative, Operation<Player> original) {
        return FeltSpawningInfluence.nearest(level, x, y, z, range, excludeCreative);
    }
}
