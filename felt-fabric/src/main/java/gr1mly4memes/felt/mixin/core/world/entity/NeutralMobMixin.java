package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.world.entity.MobBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import org.bukkit.event.entity.EntityTargetEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NeutralMob.class)
public interface NeutralMobMixin extends MobBridge {

    // @formatter:off
    @Shadow void setLastHurtByMob(@Nullable LivingEntity livingBase);
    @Shadow void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> target);
    @Shadow void setTarget(@Nullable LivingEntity entitylivingbaseIn);
    @Shadow void setPersistentAngerEndTime(long time);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default void stopBeingAngry() {
        this.setLastHurtByMob(null);
        this.setPersistentAngerTarget(null);
        this.bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
        this.setTarget(null);
        this.setPersistentAngerEndTime(-1L);
    }

    @Decorate(method = "readPersistentAngerSaveData", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/NeutralMob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void felt$targetReason() {
        if (this instanceof MobBridge b) {
            b.bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.UNKNOWN, false);
        }
    }

    default boolean setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        this.bridge$pushGoalTargetReason(reason, fireEvent);
        this.setTarget(livingEntity);
        return this.bridge$lastGoalTargetResult();
    }

    default boolean setTarget(@Nullable LivingEntity entityliving, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (this instanceof MobBridge b) {
            b.bridge$pushGoalTargetReason(reason, fireEvent);
        }
        this.setTarget(entityliving);
        if (this instanceof MobBridge b) {
            return b.bridge$lastGoalTargetResult();
        } else {
            return true;
        }
    }
}
