package gr1mly4memes.felt.mixin.core.world.effect;

import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityEffectTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class HealOrHarmMobEffectTickEventMixin {

    @Decorate(method = "applyInstantaneousEffect(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V",
        require = 1, allow = 1, inject = true, at = @At("HEAD"))
    private void felt$healOrHarmEffectTickEvent(ServerLevel level, Entity source, Entity owner,
                                                  LivingEntity target, int amplification, double scale) throws Throwable {
        // This override never calls MobEffect.applyInstantaneousEffect, so it has
        // its own producer, before either healing or damage and their Bukkit hooks.
        if (!new EntityEffectTickEvent(
            ((LivingEntityBridge) target).bridge$getBukkitEntity(),
            CraftPotionEffectType.minecraftToBukkit((MobEffect) (Object) this), amplification
        ).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
