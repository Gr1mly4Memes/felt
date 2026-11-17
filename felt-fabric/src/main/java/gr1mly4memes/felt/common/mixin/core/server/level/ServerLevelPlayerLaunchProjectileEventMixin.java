package gr1mly4memes.felt.common.mixin.core.server.level;

import gr1mly4memes.felt.common.bridge.core.world.entity.projectile.ProjectileSpawnBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelPlayerLaunchProjectileEventMixin {

    @Decorate(method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", inject = true, at = @At("HEAD"))
    private void arclight$playerLaunchBeforeAdmission(Entity entity) throws Throwable {
        if (entity instanceof ProjectileSpawnBridge projectile && !projectile.bridge$callLaunchEvent()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
    }
}
