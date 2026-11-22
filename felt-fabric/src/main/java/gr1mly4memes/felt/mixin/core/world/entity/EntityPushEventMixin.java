package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

/** Attack-attributed push only; ordinary collision and unowned pushes stay native. */
@Mixin(Entity.class)
public abstract class EntityPushEventMixin {

    @Unique
    private transient Entity felt$attackPushSource;

    // The native finite-input branch has already passed here. Dispatch before reading
    // current velocity, so plugin velocity changes and edited increments both survive.
    @Decorate(method = "push(DDD)V", inject = true, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"),
        require = 1, allow = 1)
    private void felt$attackPush(double x, double y, double z) throws Throwable {
        Entity attacker = this.felt$attackPushSource;
        // Claim before dispatch: an ordinary push from a listener is not this attack.
        this.felt$attackPushSource = null;
        if (attacker != null) {
            var event = new EntityPushedByEntityAttackEvent(
                ((EntityBridge) this).bridge$getBukkitEntity(), EntityKnockbackEvent.Cause.PUSH,
                ((EntityBridge) attacker).bridge$getBukkitEntity(), new Vector(x, y, z));
            if (!event.callEvent()) {
                // Return only from Entity.push, before velocity and needsSync writes.
                DecorationOps.cancel().invoke();
                return;
            }
            Vector knockback = event.getKnockback();
            x = knockback.getX();
            y = knockback.getY();
            z = knockback.getZ();
        }
        DecorationOps.blackhole().invoke(x, y, z);
    }

    @Mixin(Player.class)
    public abstract static class PlayerPush {

        @Decorate(method = "causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"),
            require = 1, allow = 1)
        private void felt$playerAttackPush(Entity receiver, double x, double y, double z) throws Throwable {
            EntityPushSourceAccessor source = (EntityPushSourceAccessor) receiver;
            Entity previous = source.felt$getAttackPushSource();
            source.felt$setAttackPushSource((Entity) (Object) this);
            try {
                // Only the non-living-target branch; keep attack slowdown and packet tails.
                DecorationOps.callsite().invoke(receiver, x, y, z);
            } finally {
                source.felt$setAttackPushSource(previous);
            }
        }
    }

    @Mixin(EnderDragon.class)
    public abstract static class DragonPush {

        @Decorate(method = "knockBack(Lnet/minecraft/server/level/ServerLevel;Ljava/util/List;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"),
            require = 1, allow = 1)
        private void felt$dragonAttackPush(Entity receiver, double x, double y, double z) throws Throwable {
            EntityPushSourceAccessor source = (EntityPushSourceAccessor) receiver;
            Entity previous = source.felt$getAttackPushSource();
            source.felt$setAttackPushSource((Entity) (Object) this);
            try {
                // Cancellation of push must not skip this target's later damage branch.
                DecorationOps.callsite().invoke(receiver, x, y, z);
            } finally {
                source.felt$setAttackPushSource(previous);
            }
        }
    }

    @Mixin(Ravager.class)
    public abstract static class RavagerPush {

        @Decorate(method = "strongKnockback(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"),
            require = 1, allow = 1)
        private void felt$ravagerAttackPush(Entity receiver, double x, double y, double z) throws Throwable {
            EntityPushSourceAccessor source = (EntityPushSourceAccessor) receiver;
            Entity previous = source.felt$getAttackPushSource();
            source.felt$setAttackPushSource((Entity) (Object) this);
            try {
                DecorationOps.callsite().invoke(receiver, x, y, z);
            } finally {
                source.felt$setAttackPushSource(previous);
            }
        }
    }

    @Mixin(AbstractArrow.class)
    public abstract static class ArrowPush {

        @Decorate(method = "doKnockback(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"),
            require = 1, allow = 1)
        private void felt$arrowAttackPush(LivingEntity receiver, double x, double y, double z) throws Throwable {
            EntityPushSourceAccessor source = (EntityPushSourceAccessor) receiver;
            Entity previous = source.felt$getAttackPushSource();
            // Paper attributes this increment to the arrow, not its owner/shooter.
            source.felt$setAttackPushSource((Entity) (Object) this);
            try {
                DecorationOps.callsite().invoke(receiver, x, y, z);
            } finally {
                source.felt$setAttackPushSource(previous);
            }
        }
    }

    @Mixin(HoglinBase.class)
    public interface HoglinPush {

        @Decorate(method = "throwTarget(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"),
            require = 1, allow = 1)
        private static void felt$hoglinAttackPush(LivingEntity receiver, double x, double y, double z,
                                                     LivingEntity body, LivingEntity target) throws Throwable {
            EntityPushSourceAccessor source = (EntityPushSourceAccessor) receiver;
            Entity previous = source.felt$getAttackPushSource();
            source.felt$setAttackPushSource(body);
            try {
                // RNG/attribute computation precedes this call; hurtMarked follows it.
                DecorationOps.callsite().invoke(receiver, x, y, z);
            } finally {
                source.felt$setAttackPushSource(previous);
            }
        }
    }

    @Mixin(SonicBoom.class)
    public abstract static class SonicBoomPush {

        @Decorate(method = "lambda$tick$2(Lnet/minecraft/world/entity/monster/warden/Warden;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"),
            require = 1, allow = 1)
        private static void felt$sonicBoomAttackPush(LivingEntity receiver, double x, double y, double z,
                                                       Warden body, ServerLevel level, LivingEntity target) throws Throwable {
            EntityPushSourceAccessor source = (EntityPushSourceAccessor) receiver;
            Entity previous = source.felt$getAttackPushSource();
            source.felt$setAttackPushSource(body);
            try {
                // Keep successful hurtServer gating, particles, sound and resistance.
                DecorationOps.callsite().invoke(receiver, x, y, z);
            } finally {
                source.felt$setAttackPushSource(previous);
            }
        }
    }
}
