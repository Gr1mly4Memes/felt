package gr1mly4memes.felt.mixin.core.world.level.block.entity.trialspawner;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(TrialSpawner.class)
public class TrialSpawnerMixin {

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "spawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean felt$cause(ServerLevel serverLevel, net.minecraft.world.entity.Entity entity, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        ((gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge)entity).bridge$markFromSpawner();
        ((WorldBridge) serverLevel).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER);
        return original.call(serverLevel, entity);
    }
}
