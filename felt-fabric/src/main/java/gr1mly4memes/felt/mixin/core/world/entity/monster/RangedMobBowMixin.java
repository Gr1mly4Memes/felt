package gr1mly4memes.felt.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractSkeleton.class, Illusioner.class})
public abstract class RangedMobBowMixin {
    @ModifyExpressionValue(method = "performRangedAttack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getWeaponHoldingHand(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Lnet/minecraft/world/InteractionHand;"))
    private InteractionHand felt$recordBowHand(InteractionHand hand, @Share("bowHand") LocalRef<InteractionHand> selected) {
        selected.set(hand);
        return hand;
    }

    @WrapOperation(method = "performRangedAttack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileUsingShoot(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;DDDFF)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile felt$prepareArrow(Projectile projectile, ServerLevel level, ItemStack ammunition, double x, double y,
                                        double z, float velocity, float uncertainty, Operation<Projectile> spawn,
                                        @Local(index = 3) ItemStack bow, @Local(argsOnly = true) float power,
                                        @Share("bowHand") LocalRef<InteractionHand> hand, @Cancellable CallbackInfo ci) {
        var arrow = (AbstractArrow) projectile;
        arrow.shoot(x, y, z, velocity, uncertainty);
        var event = CraftEventFactory.callEntityShootBowEvent((LivingEntity) (Object) this, bow, arrow.getPickupItem(),
            arrow, hand.get(), power, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            ci.cancel();
        } else if (event.getProjectile() == arrow.getBukkitEntity() && level.addFreshEntity(arrow)) {
            arrow.applyOnProjectileSpawned(level, ammunition);
        }
        return arrow;
    }
}
