package gr1mly4memes.felt.mixin.vanilla.world.entity;

import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felt.bridge.vanilla.world.entity.LivingEntityBridge_Vanilla;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felt.mod.util.FeltDamageContainer;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Vanilla extends EntityMixin_Vanilla implements LivingEntityBridge, LivingEntityBridge_Vanilla {

    @Override
    public boolean bridge$capturePlayerDrop(ItemEntity item) {
        return (Object) this instanceof ServerPlayer && !bridge$isForceDrops() && this.felt$captureDrop(item);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void felt$startCapture(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        this.felt$startCaptureDrops();
    }

    @Inject(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"))
    private void felt$stopCapture(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        final var list = this.felt$finishCaptureDrops();
        this.felt$vanilla$callLivingDropsEvent(damageSource, list);
        list.forEach(serverLevel::addFreshEntity);
    }

    @Decorate(method = "hurtServer", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"))
    private void felt$entityDamageEvent(ServerLevel level, DamageSource damagesource, float originalDamage, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        EntityDamageEvent event = felt$fireEntityDamageEvent(damagesource, originalDamage);

        if (event == null || event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }

        container = new FeltDamageContainer(event);
        originalDamage = (float) event.getDamage();
        DecorationOps.blackhole().invoke(container, originalDamage);

    }

    // Special handle; assuming shield cut out at most BLOCKING damage from original and g means the damage that will be used to calculate exact blocking.
    // Merged Bukkit & Modded calculation result with the above assumption. Be careful.
    /*
    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private void felt$vanilla$preApplyShield(DamageSource source, float original, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        double blocking = event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
        if (blocking == 0.0F) return;
        original = -(float) blocking;
        DecorationOps.blackhole().invoke(original);
    }
    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private void felt$vanilla$postApplyShield(DamageSource source, float original, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        double before = -event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
        if (before == -0.0F) return;
        double actualOffset = before - original;
        event.setDamage(EntityDamageEvent.DamageModifier.BLOCKING, actualOffset);
    }
    */

    @Decorate(method = "hurtServer", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;applyItemBlocking(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private void felt$vanilla$postApplyShield(ServerLevel level, DamageSource source, float original, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.BLOCKING, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurtServer", inject = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private void felt$vanilla$postApplyFreezing(ServerLevel level, DamageSource source, float original, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.FREEZING, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurtServer", inject = true, at = @At(value = "INVOKE", target = "Ljava/lang/Float;isNaN(F)Z"))
    private void felt$vanilla$postApplyHardHat(ServerLevel level, DamageSource source, float original, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.HARD_HAT, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurtServer", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void felt$vanilla$captureEntityDamageEvent(ServerLevel level, DamageSource source, float original, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        FeltCaptures.captureDamageContainer(container);
    }

    @Decorate(method = "actuallyHurt", inject = true, at = @At("HEAD"))
    private void felt$vanilla$getEntityDamageEvent(ServerLevel level, DamageSource damageSource, float f, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        container = FeltCaptures.getDamageContainer();
        DecorationOps.blackhole().invoke(container);
    }

    @Decorate(method = "actuallyHurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private void felt$vanilla$postApplyArmor(ServerLevel level, DamageSource source, float original, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.ARMOR, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float felt$vanilla$postApplyResistance(float first, float second) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = FeltCaptures.getDamageContainer().calculateStage(EntityDamageEvent.DamageModifier.RESISTANCE, result);
        return result;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float felt$vanilla$postApplyMagic(LivingEntity entity, DamageSource source, float original, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, original);
        return container.calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float felt$vanilla$postApplyAbsorption(float first, float second, @Local(allocate = "feltDamageContainer") FeltDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = container.calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, result);
        return Math.max(result, 0.0F);
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void felt$vanilla$popEntityDamageEvent(ServerLevel level, DamageSource arg, float g, CallbackInfo ci) {
        FeltCaptures.popDamageContainer();
    }
}
