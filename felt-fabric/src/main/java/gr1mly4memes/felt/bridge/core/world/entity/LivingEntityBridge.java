package gr1mly4memes.felt.bridge.core.world.entity;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface LivingEntityBridge extends EntityBridge {
    void detectEquipmentUpdatesPublic();

    List<ItemStack> bridge$collectShearDrops(ServerLevel level, ResourceKey<LootTable> table, ItemStack tool, boolean split);

    default boolean bridge$capturePlayerDrop(net.minecraft.world.entity.item.ItemEntity item) {
        return false;
    }

    org.bukkit.craftbukkit.attribute.CraftAttributeMap bridge$getCraftAttributes();

    void bridge$setSlot(EquipmentSlot slotIn, ItemStack stack, boolean silent);

    void bridge$playEquipSound(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, boolean silent);

    boolean bridge$canPickUpLoot();

    int bridge$getExpReward(Entity entity);

    void bridge$setExpToDrop(int amount);

    int bridge$getExpToDrop();

    int bridge$getInvulnerableDuration();

    void bridge$setInvulnerableDuration(int ticks);

    void bridge$pushHealReason(EntityRegainHealthEvent.RegainReason regainReason);

    /** Reads the pending one-shot reason without consuming it; null means none. */
    @Nullable
    EntityRegainHealthEvent.RegainReason bridge$peekHealReason();

    void bridge$heal(float healAmount, EntityRegainHealthEvent.RegainReason regainReason);

    void bridge$pushEffectCause(EntityPotionEffectEvent.Cause cause);

    boolean bridge$addEffect(MobEffectInstance effect, EntityPotionEffectEvent.Cause cause);

    boolean bridge$removeEffect(Holder<MobEffect> effect, EntityPotionEffectEvent.Cause cause);

    boolean bridge$removeAllEffects(EntityPotionEffectEvent.Cause cause);

    Optional<EntityPotionEffectEvent.Cause> bridge$getEffectCause();

    /** Fires the one Bukkit pre-removal decision for a bulk-clear candidate. */
    boolean bridge$willRemoveEffect(MobEffectInstance effect);

    void bridge$pushKnockbackCause(Entity attacker, EntityKnockbackEvent.KnockbackCause cause);

    @Nullable
    Vec3 bridge$fireKnockbackEvent(double force, Vec3 knockback);

    @Override
    default CraftLivingEntity getBukkitEntity() {
        return bridge$getBukkitEntity();
    }

    @Override
    CraftLivingEntity bridge$getBukkitEntity();

    enum LivingTargetType {
        BEHAVIOR_TARGET,
        MOB_TARGET
    }

    default int bridge$mod$getExperienceDrop(LivingEntity entity, Player attackingPlayer, int originalExperience) {
        return originalExperience;
    }

    default boolean bridge$mod$onLivingUseTotem(LivingEntity entity, DamageSource damageSource, ItemStack totem, InteractionHand hand) {
        return true;
    }

    default void bridge$mod$onLivingConvert(LivingEntity entity, LivingEntity outcome) {}

    default boolean bridge$mod$canEntityDestroy(Level level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    @Nullable
    EntityDamageEvent felt$fireEntityDamageEvent(DamageSource source, float original);

    default CraftLivingEntity getBukkitLivingEntity() {
        return this.bridge$getBukkitEntity();
    }

    default void heal(float amount, EntityRegainHealthEvent.RegainReason reason) {
        this.bridge$heal(amount, reason);
    }

    default boolean addEffect(MobEffectInstance effect, EntityPotionEffectEvent.Cause cause) {
        return this.bridge$addEffect(effect, cause);
    }

    default boolean removeEffect(Holder<MobEffect> effect, EntityPotionEffectEvent.Cause cause) {
        return this.bridge$removeEffect(effect, cause);
    }

    default boolean removeAllEffects(EntityPotionEffectEvent.Cause cause) {
        return this.bridge$removeAllEffects(cause);
    }

    default void setItemSlot(EquipmentSlot slot, ItemStack stack, boolean silent) {
        this.bridge$setSlot(slot, stack, silent);
    }

}
