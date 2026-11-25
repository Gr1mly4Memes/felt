package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityVoidDamageMixin {
    @Shadow protected abstract void onBelowWorld();
    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void felt$configuredVoidDamage(CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if (!(entity.level() instanceof ServerLevel level)) return;
        ci.cancel();
        var world = ((WorldBridge)level).bridge$getWorld();
        if (!world.isVoidDamageEnabled()) return;
        boolean below = entity.getY() < level.getMinY() + world.getVoidDamageMinBuildHeightOffset();
        boolean ceiling = world.getEnvironment() == org.bukkit.World.Environment.NETHER
            && ((WorldBridge)level).bridge$paperConfig().environment.netherCeilingVoidDamageHeight.test(height -> entity.getY() >= height)
            && (!(entity instanceof Player player) || !player.getAbilities().invulnerable);
        if (below || ceiling) this.onBelowWorld();
    }
}
