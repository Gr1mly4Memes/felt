package gr1mly4memes.felt.mixin.core.world.entity.projectile;

import gr1mly4memes.felt.bridge.core.world.entity.projectile.AbstractHurtingProjectileBridge;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin extends ProjectileMixin implements AbstractHurtingProjectileBridge {

    public float bukkitYield;
    public boolean isIncendiary;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void felt$init(EntityType<? extends AbstractHurtingProjectile> p_i50173_1_, Level p_i50173_2_, CallbackInfo ci) {
        this.bukkitYield = 1;
        this.isIncendiary = true;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/AbstractHurtingProjectile;discard()V"))
    private void felt$despawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Override
    public void bridge$setBukkitYield(float yield) {
        this.bukkitYield = yield;
    }
}
