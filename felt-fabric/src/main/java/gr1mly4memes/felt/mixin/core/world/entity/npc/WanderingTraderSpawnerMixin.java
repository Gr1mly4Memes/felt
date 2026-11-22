package gr1mly4memes.felt.mixin.core.world.entity.npc;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTraderSpawner.class)
public class WanderingTraderSpawnerMixin {

    @Redirect(method = "spawn(Lnet/minecraft/server/level/ServerLevel;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;setDespawnDelay(I)V"), require = 1, expect = 1, allow = 1)
    private void felt$preserveDespawnDelay(WanderingTrader trader, int delay) {
        // The constructor supplies the default; retain trader and llama spawn listener edits.
    }

    @Inject(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
    public void felt$spawnReason1(ServerLevel serverWorld, CallbackInfoReturnable<Boolean> cir) {
        ((WorldBridge) serverWorld).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }

    @Inject(method = "tryToSpawnLlamaFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
    public void felt$spawnReason2(ServerLevel serverWorld, WanderingTrader p_242373_2_, int p_242373_3_, CallbackInfo ci) {
        ((WorldBridge) serverWorld).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }
}
