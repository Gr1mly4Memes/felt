package gr1mly4memes.felt.mixin.core.world.entity.projectile.hurtingprojectile.windcharge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({WindCharge.class, BreezeWindCharge.class})
public abstract class WindChargeExplosionMixin {
    @WrapOperation(method = "explode", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/util/random/WeightedList;Lnet/minecraft/core/Holder;)V"))
    private void felt$primeWindExplosion(Level level, Entity entity, DamageSource source,
                                         ExplosionDamageCalculator calculator, double x, double y, double z, float radius,
                                         boolean fire, Level.ExplosionInteraction interaction, ParticleOptions small,
                                         ParticleOptions large, WeightedList<ExplosionParticleInfo> blockParticles,
                                         Holder<SoundEvent> sound, Operation<Void> original) {
        if (level instanceof ServerLevel) {
            var event = CraftEventFactory.callExplosionPrimeEvent(entity, radius, fire);
            if (event.isCancelled()) {
                return;
            }
            radius = event.getRadius();
            fire = event.getFire();
        }
        original.call(level, entity, source, calculator, x, y, z, radius, fire, interaction, small, large, blockParticles, sound);
    }
}
