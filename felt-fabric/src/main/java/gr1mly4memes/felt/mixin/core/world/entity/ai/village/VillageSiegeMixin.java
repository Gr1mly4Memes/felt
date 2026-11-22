package gr1mly4memes.felt.mixin.core.world.entity.ai.village;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.VillageSiege;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillageSiege.class)
public class VillageSiegeMixin {

    @Inject(method = "trySpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void felt$spawnReason(ServerLevel level, CallbackInfo ci) {
        ((WorldBridge) level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION);
    }
}
