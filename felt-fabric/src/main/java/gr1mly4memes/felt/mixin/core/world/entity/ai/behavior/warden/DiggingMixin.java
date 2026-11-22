package gr1mly4memes.felt.mixin.core.world.entity.ai.behavior.warden;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.warden.Digging;
import net.minecraft.world.entity.monster.warden.Warden;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Digging.class)
public class DiggingMixin<E extends Warden> {

    @Inject(method = "stop(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;J)V", at = @At("HEAD"))
    private void felt$despawn(ServerLevel serverLevel, E warden, long l, CallbackInfo ci) {
        warden.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }
}
