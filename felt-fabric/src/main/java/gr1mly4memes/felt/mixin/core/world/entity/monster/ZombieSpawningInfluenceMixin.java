package gr1mly4memes.felt.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.mod.server.world.FeltSpawningInfluence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Zombie.class)
public abstract class ZombieSpawningInfluenceMixin {
    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;hasNearbyAlivePlayer(DDDD)Z"))
    private boolean felt$reinforcementPlayer(ServerLevel level, double x, double y, double z, double range, Operation<Boolean> original) {
        return FeltSpawningInfluence.nearbyAlive(level, x, y, z, range);
    }
}
