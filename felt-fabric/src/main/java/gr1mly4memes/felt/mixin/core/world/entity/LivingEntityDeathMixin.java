package gr1mly4memes.felt.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.world.entity.DeathEventBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.event.entity.EntityDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin implements DeathEventBridge {
    @Shadow protected boolean dead;
    @Shadow protected abstract SoundEvent getDeathSound();
    @Shadow protected abstract float getSoundVolume();
    @Shadow public abstract float getVoicePitch();
    @org.spongepowered.asm.mixin.gen.Invoker("playSecondaryHurtSound")
    protected abstract void felt$playSecondaryHurtSound(DamageSource source);
    @Shadow protected abstract void dropAllDeathLoot(ServerLevel level, DamageSource source);
    @Unique private EntityDeathEvent felt$deathEvent;
    @Unique private boolean silentDeath; // Paper added
    @Unique private boolean felt$fullDeathSound;
    @Unique private boolean felt$deathLootMoved;
    @Unique private final List<Runnable> felt$deathChanges = new ArrayList<>();

    @Override
    public void bridge$resetDeathEvent() {
        this.felt$deathEvent = null;
        this.felt$deathChanges.clear();
    }

    @Override
    public boolean bridge$deathCancelled() {
        return this.felt$deathEvent != null && this.felt$deathEvent.isCancelled();
    }

    @Override
    public void bridge$setSilentDeath(boolean silent) {
        this.silentDeath = silent;
    }

    @Override
    public void bridge$deferDeathChange(Runnable action) {
        this.felt$deathChanges.add(action);
    }

    @Override
    public void bridge$prepareDeathEvent(EntityDeathEvent event) {
        LivingEntity self = (LivingEntity) (Object) this;
        event.setReviveHealth(event.getEntity().getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        event.setShouldPlayDeathSound(!this.silentDeath && !self.isSilent());
        SoundEvent sound = this.getDeathSound();
        event.setDeathSound(sound == null ? null : CraftSound.minecraftToBukkit(sound));
        event.setDeathSoundCategory(org.bukkit.SoundCategory.valueOf(self.getSoundSource().name()));
        event.setDeathSoundVolume(this.getSoundVolume());
        event.setDeathSoundPitch(this.getVoicePitch());
    }

    @Override
    public void bridge$finishDeathEvent(EntityDeathEvent event, DamageSource source) {
        this.felt$deathEvent = event;
        LivingEntity self = (LivingEntity) (Object) this;
        if (event.isCancelled()) {
            this.felt$deathChanges.clear();
            this.dead = false;
            if (!(self instanceof ServerPlayer) || self.getHealth() <= 0) self.setHealth((float) event.getReviveHealth());
            ((gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge) self).bridge$setExpToDrop(0);
            return;
        }
        if (event.shouldPlayDeathSound() && event.getDeathSound() != null && event.getDeathSoundCategory() != null) {
            self.level().playSound(self instanceof net.minecraft.world.entity.player.Player player ? player : null,
                self.getX(), self.getY(), self.getZ(), CraftSound.bukkitToMinecraft(event.getDeathSound()),
                net.minecraft.sounds.SoundSource.valueOf(event.getDeathSoundCategory().name()), event.getDeathSoundVolume(), event.getDeathSoundPitch());
            this.felt$playSecondaryHurtSound(source);
        }
        if (this.felt$deathLootMoved) {
            LivingEntity killer = self.getKillCredit();
            if (killer != null) killer.awardKillScore(self, source);
        }
        var changes = List.copyOf(this.felt$deathChanges);
        this.felt$deathChanges.clear();
        changes.forEach(Runnable::run);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void felt$beginSound(ServerLevel level, DamageSource source, float damage,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        this.felt$fullDeathSound = false;
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;makeSound(Lnet/minecraft/sounds/SoundEvent;)V"))
    private void felt$delayDeathSound(LivingEntity entity, SoundEvent sound, Operation<Void> original) {
        this.felt$fullDeathSound = true;
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/world/entity/LivingEntity;playSecondaryHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void felt$delaySecondary(LivingEntity entity, DamageSource source, Operation<Void> original) {}

    @Decorate(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;die(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void felt$deathSoundScope(LivingEntity entity, DamageSource source) throws Throwable {
        this.silentDeath = !this.felt$fullDeathSound;
        try {
            DecorationOps.callsite().invoke(entity, source);
        } finally {
            this.silentDeath = false;
        }
    }

    @Decorate(method = "die", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getKillCredit()Lnet/minecraft/world/entity/LivingEntity;"))
    private void felt$deathBeforeEffects(DamageSource source) throws Throwable {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;
        this.bridge$resetDeathEvent();
        this.dead = true;
        this.felt$deathLootMoved = true;
        if (source.getEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper) creeper.killedEntity(level, self, source);
        this.dropAllDeathLoot(level, source);
        if (this.bridge$deathCancelled()) {
            if (source.getEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper) {
                ((CreeperDeathAccessor) creeper).felt$droppedSkulls(false);
            }
            this.felt$deathLootMoved = false;
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Decorate(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void felt$alreadyDropped(LivingEntity entity, ServerLevel level, DamageSource source) throws Throwable {
        if (!this.felt$deathLootMoved) DecorationOps.callsite().invoke(entity, level, source);
    }

    @Decorate(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;awardKillScore(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void felt$alreadyAwarded(LivingEntity killer, Entity victim, DamageSource source) throws Throwable {
        if (!this.felt$deathLootMoved) DecorationOps.callsite().invoke(killer, victim, source);
    }

    @Decorate(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;killedEntity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean felt$acceptedKill(Entity killer, ServerLevel level, LivingEntity victim, DamageSource source) throws Throwable {
        boolean killed = (boolean) DecorationOps.callsite().invoke(killer, level, victim, source);
        return true;
    }

    @WrapOperation(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;stopUsingItem()V"))
    private void felt$paperStopUsing(LivingEntity entity, Operation<Void> original) {}

    @Inject(method = "die", at = @At("RETURN"))
    private void felt$endDeath(DamageSource source, CallbackInfo ci) {
        this.felt$deathLootMoved = false;
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void felt$beginLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        this.felt$deathEvent = null;
    }
}
