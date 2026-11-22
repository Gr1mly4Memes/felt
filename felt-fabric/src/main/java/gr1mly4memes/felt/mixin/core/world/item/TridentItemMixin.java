package gr1mly4memes.felt.mixin.core.world.item;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gr1mly4memes.felt.bridge.core.world.entity.projectile.ThrownTridentBridge;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felt.mod.util.DistValidate;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    @Decorate(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtWithoutBreaking(ILnet/minecraft/world/entity/player/Player;)V"))
    private void felt$muteDamage(ItemStack instance, int i, Player player, @Local(ordinal = -1) float f) throws Throwable {
        if (f != 0) {
            DecorationOps.callsite().invoke(instance, i, player);
        }
    }

    @WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consumeAndReturn(ILnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack felt$deferConsumption(ItemStack stack, int amount, LivingEntity entity, Operation<ItemStack> original,
            @Share("felt$consume") LocalRef<Operation<ItemStack>> consume) {
        consume.set(original);
        return stack.copyWithCount(amount);
    }

    @WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation(Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile felt$addEntity(Projectile.ProjectileFactory<? extends Projectile> factory, ServerLevel serverLevel,
            ItemStack itemStack, LivingEntity livingEntity, float xRot, float velocity, float inaccuracy, Operation<Projectile> original,
            ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft,
            @Share("felt$consume") LocalRef<Operation<ItemStack>> consume,
            @Cancellable CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) entityLiving;
        Projectile entityIn;
        var launch = new org.apache.commons.lang3.mutable.MutableObject<PlayerLaunchProjectileEvent>();
        var createdProjectile = new org.apache.commons.lang3.mutable.MutableObject<Projectile>();
        try {
            entityIn = original.call((Projectile.ProjectileFactory<Projectile>) (spawnLevel, shooter, item) -> {
            Projectile created = factory.create(spawnLevel, shooter, item);
            createdProjectile.setValue(created);
            FeltEventFactory.prepareProjectileLaunch(created, player, stack, launch::setValue);
            return created;
            }, serverLevel, itemStack, livingEntity, xRot, velocity, inaccuracy);
        } finally {
            if (createdProjectile.getValue() != null) {
                ((gr1mly4memes.felt.bridge.core.world.entity.projectile.ProjectileSpawnBridge) createdProjectile.getValue()).bridge$setLaunchEvent(null);
            }
        }
        PlayerLaunchProjectileEvent event = launch.getValue();
        if (!FeltEventFactory.wasProjectileAccepted(entityIn)) {
            FeltEventFactory.resyncProjectileItem(player, player.getUsedItemHand());
            cir.setReturnValue(false);
            return entityIn;
        }
        if (event == null || event.shouldConsume()) {
            itemStack.hurtWithoutBreaking(1, player);
        }
        ((ThrownTridentBridge) entityIn).bridge$setThrownStack(itemStack.copy());
        if (event == null || event.shouldConsume()) {
            consume.get().call(stack, 1, entityLiving);
        }
        return entityIn;
    }

    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;push(DDD)V"))
    private void felt$riptide(Player instance, double x, double y, double z, ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        if (!DistValidate.isValid(worldIn)) return;
        CraftEventFactory.callPlayerRiptideEvent(instance, stack, (float) x, (float) y, (float) z);
        instance.push(x, y, z);
    }
}
