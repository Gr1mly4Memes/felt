package gr1mly4memes.felt.mixin.core.world.entity.decoration;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.mixin.core.world.entity.LivingEntityMixin;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(net.minecraft.world.entity.decoration.ArmorStand.class)
public abstract class ArmorStandMixin extends LivingEntityMixin {

    // @formatter:off
    @Shadow private boolean invisible;
    // @formatter:on

    @Override
    public float getBukkitYaw() {
        return this.getYRot();
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/decoration/ArmorStand;kill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"))
    public void felt$damageDropOut(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((net.minecraft.world.entity.decoration.ArmorStand) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;IS_EXPLOSION:Lnet/minecraft/tags/TagKey;"))
    public void felt$damageNormal(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((net.minecraft.world.entity.decoration.ArmorStand) (Object) this, source, amount, true, this.invisible)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "hurtServer", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;invisible:Z"))
    private boolean felt$softenCondition(net.minecraft.world.entity.decoration.ArmorStand entity) {
        return false;
    }

    private boolean felt$preparedDeath;
    private net.minecraft.world.entity.item.ItemEntity felt$standDrop;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void felt$resetDeath(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        this.felt$preparedDeath = false;
        this.felt$standDrop = null;
        ((gr1mly4memes.felt.bridge.core.world.entity.DeathEventBridge) this).bridge$resetDeathEvent();
    }

    @Inject(method = "kill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void felt$pushRemoveCause(ServerLevel level, Entity attributedTo, CallbackInfo ci) {
        var death = (gr1mly4memes.felt.bridge.core.world.entity.DeathEventBridge) this;
        if (!this.felt$preparedDeath) {
            death.bridge$resetDeathEvent();
            CraftEventFactory.callEntityDeathEvent((net.minecraft.world.entity.LivingEntity) (Object) this, this.damageSources().genericKill());
        }
        this.felt$preparedDeath = false;
        if (death.bridge$deathCancelled()) {
            ci.cancel();
            return;
        }
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "brokenByPlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void felt$captureStand(Level level, BlockPos pos, ItemStack stack,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        this.felt$standDrop = this.felt$spawnAtLocationNoAdd(stack);
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "brokenByAnything", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityEquipment;set(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack felt$deferClear(net.minecraft.world.entity.EntityEquipment equipment,
            net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<ItemStack> original) {
        ItemStack previous = equipment.get(slot);
        ((gr1mly4memes.felt.bridge.core.world.entity.DeathEventBridge) this)
            .bridge$deferDeathChange(() -> original.call(equipment, slot, stack));
        return previous;
    }

    @Redirect(method = "brokenByAnything", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void felt$dropLater(net.minecraft.world.entity.decoration.ArmorStand instance, ServerLevel serverLevel, DamageSource damageSource) {
        var drops = new ArrayList<net.minecraft.world.entity.item.ItemEntity>(6);
        if (this.felt$standDrop != null) {
            drops.add(this.felt$standDrop);
            this.felt$standDrop = null;
        }
        FeltCaptures.captureExtraDrops(drops);
    }

    @Decorate(method = "brokenByAnything", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void felt$captureDropsDeath(Level worldIn, BlockPos pos, ItemStack stack) throws Throwable {
        try {
            FeltCaptures.captureBlockPopResForExtraDrops(true);
            DecorationOps.callsite().invoke(worldIn, pos, stack);
        } finally {
            FeltCaptures.captureBlockPopResForExtraDrops(false);
        }
    }

    @Inject(method = "brokenByAnything", at = @At("RETURN"))
    private void felt$spawnLast(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        this.dropAllDeathLoot(serverLevel, damageSource);
        this.felt$preparedDeath = true;
    }

    @Override
    protected boolean shouldDropExperience() {
        return true;
    }


    @Inject(method = "swapItem", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z"))
    public void felt$manipulateEvent(net.minecraft.world.entity.player.Player playerEntity, net.minecraft.world.entity.EquipmentSlot slotType, ItemStack itemStack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        ItemStack itemStack1 = this.getItemBySlot(slotType);

        org.bukkit.inventory.ItemStack armorStandItem = CraftItemStack.asCraftMirror(itemStack1);
        org.bukkit.inventory.ItemStack playerHeldItem = CraftItemStack.asCraftMirror(itemStack);

        Player player = ((ServerPlayerBridge) playerEntity).bridge$getBukkitEntity();
        ArmorStand self = (ArmorStand) ((EntityBridge) this).bridge$getBukkitEntity();

        EquipmentSlot slot = CraftEquipmentSlot.getSlot(slotType);
        EquipmentSlot bukkitHand = CraftEquipmentSlot.getHand(hand);
        PlayerArmorStandManipulateEvent event = new PlayerArmorStandManipulateEvent(player, self, playerHeldItem, armorStandItem, slot, bukkitHand);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack, boolean silent) {
        switch (slot.getType()) {
            case HAND, HUMANOID_ARMOR -> this.bridge$playEquipSound(slot, this.equipment.set(slot, stack), stack, silent);
        }
    }
}
