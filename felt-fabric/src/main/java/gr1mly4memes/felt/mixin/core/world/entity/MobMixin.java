package gr1mly4memes.felt.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.phys.Vec3;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felt.bridge.core.world.entity.MobBridge;
import gr1mly4memes.felt.bridge.core.world.entity.ai.goal.GoalSelectorBridge;
import gr1mly4memes.felt.bridge.core.world.entity.animal.PigBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.mod.server.FeltServer;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntityMixin implements MobBridge {
    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getNearestPlayer(Lnet/minecraft/world/entity/Entity;D)Lnet/minecraft/world/entity/player/Player;"))
    private Player felt$spawningPlayerForDespawn(net.minecraft.world.level.Level level, Entity source, double range, Operation<Player> original) {
        return gr1mly4memes.felt.mod.server.world.FeltSpawningInfluence.nearestForDespawn(level, source, range);
    }
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;pickUpItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/item/ItemEntity;)V"))
    private void felt$mobPickupControl(Mob mob, ServerLevel level, ItemEntity item, Operation<Void> original) {
        if (((gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge)item).bridge$canMobPickup()) original.call(mob, level, item);
    }

    @WrapOperation(method = "shearItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity felt$forcedEquipmentDrop(Mob entity, ServerLevel level, ItemStack stack, Vec3 offset, Operation<ItemEntity> original) {
        boolean previous = this.forceDrops;
        this.forceDrops = true;
        try {
            return original.call(entity, level, stack, offset);
        } finally {
            this.forceDrops = previous;
        }
    }

    // @formatter:off
    @Shadow public boolean persistenceRequired;
    @Shadow public abstract boolean removeWhenFarAway(double distanceToClosestPlayer);
    @Shadow @Nullable public abstract LivingEntity getTarget();
    @Shadow private LivingEntity target;
    @Shadow public abstract boolean canHoldItem(ItemStack stack);
    @Shadow public abstract DropChances getDropChances();
    @Shadow public abstract void setDropChance(EquipmentSlot equipmentSlot, float chance);
    @Shadow public abstract boolean isPersistenceRequired();
    @Shadow public abstract boolean isNoAi();
    @Shadow protected abstract boolean canReplaceCurrentItem(ItemStack candidate, ItemStack existing, EquipmentSlot slot);
    @Shadow protected abstract void setItemSlotAndDropWhenKilled(EquipmentSlot p_233657_1_, ItemStack p_233657_2_);
    @Shadow @Nullable public abstract <T extends Mob> T convertTo(EntityType<T> entityType, ConversionParams conversionParams, EntitySpawnReason spawnReason, ConversionParams.AfterConversion<T> afterConversion);
    @Shadow @Nullable protected abstract SoundEvent getAmbientSound();
    @Shadow public abstract void setTarget(@org.jetbrains.annotations.Nullable LivingEntity livingEntity);
    // @formatter:on

    @Shadow @org.spongepowered.asm.mixin.Final protected net.minecraft.world.entity.ai.goal.GoalSelector goalSelector;
    @Shadow @org.spongepowered.asm.mixin.Final protected net.minecraft.world.entity.ai.goal.GoalSelector targetSelector;

    @Override
    public net.minecraft.world.entity.ai.goal.GoalSelector bridge$goalSelector() {
        return this.goalSelector;
    }

    @Override
    public net.minecraft.world.entity.ai.goal.GoalSelector bridge$targetSelector() {
        return this.targetSelector;
    }

    public boolean aware;

    @Override
    public void bridge$inactiveTick() {
        super.bridge$inactiveTick();
        if (!this.aware) {
            return;
        }
        if (((GoalSelectorBridge) this.goalSelector).bridge$inactiveTick()) {
            this.goalSelector.tick();
        }
        if (((GoalSelectorBridge) this.targetSelector).bridge$inactiveTick()) {
            this.targetSelector.tick();
        }
    }

    @Override
    public void bridge$setAware(boolean aware) {
        this.aware = aware;
    }

    @Inject(method = "setCanPickUpLoot", at = @At("HEAD"))
    public void felt$setPickupLoot(boolean canPickup, CallbackInfo ci) {
        super.bukkitPickUpLoot = canPickup;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canPickUpLoot() {
        return super.bukkitPickUpLoot;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$init(EntityType<? extends Mob> type, net.minecraft.world.level.Level worldIn, CallbackInfo ci) {
        this.aware = true;
    }

    public SoundEvent getAmbientSound0() {
        return getAmbientSound();
    }

    protected transient boolean felt$targetSuccess = false;
    private transient EntityTargetEvent.TargetReason felt$reason;
    private transient boolean felt$fireEvent;
    private transient EntityTargetEvent.TargetReason felt$activeTargetReason;
    private transient boolean felt$activeTargetEvent;
    private transient boolean felt$activeTargetSuccess;

    @WrapMethod(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V")
    private void felt$targetScope(LivingEntity livingEntity, Operation<Void> original) {
        EntityTargetEvent.TargetReason parentReason = felt$activeTargetReason;
        boolean parentEvent = felt$activeTargetEvent;
        boolean parentSuccess = felt$activeTargetSuccess;
        felt$activeTargetReason = felt$reason;
        felt$activeTargetEvent = felt$fireEvent;
        felt$activeTargetSuccess = false;
        felt$reason = null;
        felt$fireEvent = false;
        try {
            original.call(livingEntity);
        } finally {
            felt$targetSuccess = felt$activeTargetSuccess;
            felt$activeTargetReason = parentReason;
            felt$activeTargetEvent = parentEvent;
            felt$activeTargetSuccess = parentSuccess;
        }
    }

    @Decorate(method = "setTarget", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/Mob;target:Lnet/minecraft/world/entity/LivingEntity;"))
    private void felt$setTargetEvent(Mob instance, LivingEntity livingEntity) throws Throwable {
        boolean fireEvent = felt$activeTargetEvent;
        felt$activeTargetEvent = false;
        EntityTargetEvent.TargetReason reason = felt$activeTargetReason == null ? EntityTargetEvent.TargetReason.UNKNOWN : felt$activeTargetReason;
        felt$activeTargetReason = null;
        // Compare the stored reference: getTarget() may hide a stale target as null.
        if (this.target == livingEntity) {
            felt$activeTargetSuccess = false;
            return;
        }
        if (fireEvent) {
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && this.getTarget() != null && livingEntity == null) {
                reason = (this.getTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED);
            }
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN) {
                FeltServer.LOGGER.debug("Unknown target reason setting {} target to {}", this, livingEntity);
            }
            CraftLivingEntity ctarget = null;
            if (livingEntity != null) {
                ctarget = ((LivingEntityBridge) livingEntity).bridge$getBukkitEntity();
            }
            final EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(this.getBukkitEntity(), ctarget, reason);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                felt$activeTargetSuccess = false;
                DecorationOps.cancel().invoke();
                return;
            }
            if (event.getTarget() != null) {
                livingEntity = ((CraftLivingEntity) event.getTarget()).getHandle();
            } else {
                livingEntity = null;
            }
        }
        DecorationOps.callsite().invoke(instance, livingEntity);
        felt$activeTargetSuccess = true;
    }

    public boolean setTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        bridge$pushGoalTargetReason(reason, fireEvent);
        setTarget(livingEntity);
        return felt$targetSuccess;
    }

    @Override
    public boolean bridge$lastGoalTargetResult() {
        return felt$targetSuccess;
    }

    @Override
    public boolean bridge$setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        return setTarget(livingEntity, reason, fireEvent);
    }

    @Override
    public void bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (fireEvent) {
            this.felt$reason = reason;
        } else {
            this.felt$reason = null;
        }
        felt$fireEvent = fireEvent;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void felt$setAware(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("Bukkit.Aware", this.aware);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void felt$readAware(ValueInput input, CallbackInfo ci) {
        this.aware = input.getBooleanOr("Bukkit.Aware", true);
    }

    @Redirect(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setCanPickUpLoot(Z)V"))
    public void felt$setIfTrue(Mob mobEntity, boolean canPickup) {
        if (canPickup) mobEntity.setCanPickUpLoot(true);
    }

    @Inject(method = "serverAiStep", cancellable = true, at = @At("HEAD"))
    private void felt$unaware(CallbackInfo ci) {
        if (!this.aware) {
            ++this.noActionTime;
            ci.cancel();
        }
    }

    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private void felt$captureItemEntity(ServerLevel level, ItemEntity itemEntity, CallbackInfo ci) {
        felt$item = itemEntity;
    }

    @Inject(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void felt$pickupCause(ServerLevel level, ItemEntity itemEntity, CallbackInfo ci) {
        itemEntity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
    }

    @Override
    public void bridge$captureItemDrop(ItemEntity itemEntity) {
        this.felt$item = itemEntity;
    }

    private transient ItemEntity felt$item;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ItemStack equipItemIfPossible(ServerLevel level, ItemStack stack) {
        ItemEntity itemEntity = felt$item;
        felt$item = null;
        EquipmentSlot equipmentslottype = getEquipmentSlotForItem(stack);
        if (!((LivingEntity) (Object) this).isEquippableInSlot(stack, equipmentslottype)) {
            return ItemStack.EMPTY;
        }
        ItemStack itemstack = this.getItemBySlot(equipmentslottype);
        boolean flag = this.canReplaceCurrentItem(stack, itemstack, equipmentslottype);

        if (equipmentslottype.isArmor() && !flag) {
            equipmentslottype = EquipmentSlot.MAINHAND;
            itemstack = this.getItemBySlot(equipmentslottype);
            flag = itemstack.isEmpty();
        }

        boolean canPickup = flag && this.canHoldItem(stack);
        if (itemEntity != null) {
            canPickup = !CraftEventFactory.callEntityPickupItemEvent((Mob) (Object) this, itemEntity, 0, !canPickup).isCancelled();
        }
        if (canPickup) {
            double d0 = this.getDropChances().byEquipment(equipmentslottype);
            if (!itemstack.isEmpty() && (double) Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
                forceDrops = true;
                this.spawnAtLocation(level, itemstack);
                forceDrops = false;
            }

            var itemstack2 = equipmentslottype.limit(stack);
            this.setItemSlotAndDropWhenKilled(equipmentslottype, itemstack2);
            return itemstack2;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;dropLeash()V"))
    private void felt$unleashRide(Entity entityIn, boolean force, boolean forcePassenger, CallbackInfoReturnable<Boolean> cir) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
    }

    private transient boolean felt$conversionEquipment;
    private transient boolean felt$conversionEquipmentDeferred;

    @WrapMethod(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;")
    private <T extends Mob> T felt$conversionEquipment(EntityType<T> entityType, ConversionParams params, EntitySpawnReason spawnReason,
            ConversionParams.AfterConversion<T> afterConversion, Operation<T> original) {
        this.felt$conversionEquipment = params.type() == net.minecraft.world.entity.ConversionType.SINGLE && params.keepEquipment();
        this.felt$conversionEquipmentDeferred = false;
        try {
            return original.call(entityType, params, spawnReason, afterConversion);
        } finally {
            this.felt$conversionEquipment = false;
            this.felt$conversionEquipmentDeferred = false;
            this.felt$transform = null;
            ((WorldBridge) this.level()).bridge$pushAddEntityReason(null);
        }
    }

    @Override
    public boolean bridge$deferConversionEquipment() {
        if (!this.felt$conversionEquipment) return false;
        this.felt$conversionEquipmentDeferred = true;
        return true;
    }

    @Override
    public void bridge$clearDeferredConversionEquipment() {
        if (this.felt$conversionEquipmentDeferred) this.bridge$clearConversionEquipment();
    }

    @Override
    public void bridge$clearConversionEquipment() {
        if (!this.felt$conversionEquipment) return;
        this.felt$conversionEquipmentDeferred = false;
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.VALUES) {
            ItemStack stack = ((Mob) (Object) this).getItemBySlot(slot);
            if (!stack.isEmpty()) stack.setCount(0);
        }
    }

    @WrapOperation(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean felt$copySpawn(ServerLevel world, Entity entityIn, Operation<Boolean> original,
            EntityType<?> type, ConversionParams params, EntitySpawnReason spawnReason,
            ConversionParams.AfterConversion<?> after,
            @com.llamalad7.mixinextras.sugar.Cancellable CallbackInfoReturnable<Mob> cir) {
        if (params.type() == net.minecraft.world.entity.ConversionType.SPLIT_ON_DEATH
            && after instanceof gr1mly4memes.felt.mod.server.event.FeltEventFactory.SlimeSplit.Conversion<?> split
            && split.defer((Mob) (Object) this, entityIn, () -> original.call(world, entityIn))) {
            return true;
        }
        EntityTransformEvent.TransformReason transformReason = felt$transform == null
            ? EntityTransformEvent.TransformReason.UNKNOWN : felt$transform;
        this.felt$transform = null;
        boolean pigZapDispatched = false;
        if ((Object) this instanceof Pig pig && entityIn instanceof ZombifiedPiglin piglin) {
            PigBridge pigBridge = (PigBridge) pig;
            var lightningBolt = pigBridge.bridge$getPigZapLightning();
            if (lightningBolt != null) {
                pigBridge.bridge$clearPigZapLightning();
                pigZapDispatched = true;
                if (CraftEventFactory.callPigZapEvent(pig, lightningBolt, piglin).isCancelled()) {
                    pigBridge.bridge$cancelPigZap();
                    cir.setReturnValue(null);
                    return false;
                }
            }
        }
        // PigZapEvent owns the lightning-pig conversion; do not dispatch a second transform event.
        if (!pigZapDispatched && CraftEventFactory.callEntityTransformEvent((Mob) (Object) this, (LivingEntity) entityIn, transformReason).isCancelled()) {
            cir.setReturnValue(null);
            return false;
        } else {
            this.bridge$clearConversionEquipment();
            return original.call(world, entityIn);
        }
    }

    @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private <T extends Mob> void felt$transformCause(EntityType<T> entityType, ConversionParams conversionParams, EntitySpawnReason spawnReason, ConversionParams.AfterConversion<T> afterConversion, CallbackInfoReturnable<T> cir) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.TRANSFORMATION);
    }

    public <T extends Mob> T convertTo(EntityType<T> entityType, boolean flag, EntityTransformEvent.TransformReason transformReason, CreatureSpawnEvent.SpawnReason spawnReason) {
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(spawnReason);
        bridge$pushTransformReason(transformReason);
        return this.convertTo(entityType, ConversionParams.single((Mob) (Object) this, flag, flag), EntitySpawnReason.CONVERSION, mob -> {});
    }

    private transient EntityTransformEvent.TransformReason felt$transform;

    @Override
    public EntityTransformEvent.TransformReason bridge$getTransformReason() {
        return this.felt$transform;
    }


    @Override
    public void bridge$pushTransformReason(EntityTransformEvent.TransformReason transformReason) {
        this.felt$transform = transformReason;
    }

    @Inject(method = "doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/damagesource/DamageSource;FZ)V"))
    private void felt$attackKnockback(ServerLevel level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntityBridge) entity).bridge$pushKnockbackCause((Entity) (Object) this, EntityKnockbackEvent.KnockbackCause.ENTITY_ATTACK);
    }

    @Inject(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private void felt$naturalDespawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Override
    public boolean bridge$isPersistenceRequired() {
        return this.persistenceRequired;
    }

    public void setPersistenceRequired(boolean value) {
        this.persistenceRequired = value;
    }

    @Override
    public void bridge$setPersistenceRequired(boolean value) {
        this.setPersistenceRequired(value);
    }

    @Override
    public boolean bridge$common$animalTameEvent(Player player) {
        return !CraftEventFactory.callEntityTameEvent((Mob) (Object) this, player).isCancelled();
    }
}
