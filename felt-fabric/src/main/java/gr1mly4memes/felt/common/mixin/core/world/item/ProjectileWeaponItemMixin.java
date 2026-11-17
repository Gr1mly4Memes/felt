package gr1mly4memes.felt.common.mixin.core.world.item;

import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ProjectileWeaponItem.class)
public class ProjectileWeaponItemMixin {

    @Decorate(method = "shoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile arclight$shootBow(Projectile projectile, ServerLevel instance, ItemStack projectileStack, Consumer<Projectile> consumer, ServerLevel serverLevel, LivingEntity livingEntity, InteractionHand interactionHand, ItemStack itemStack,
                                         List<ItemStack> list, float f, float g, boolean bl, LivingEntity target) throws Throwable {
        consumer.accept(projectile);
        Consumer<Projectile> alreadyShot = ignored -> {};
        var event = CraftEventFactory.callEntityShootBowEvent(livingEntity, itemStack, projectileStack, projectile, interactionHand, f, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            return (Projectile) DecorationOps.cancel().invoke();
        }

        if (event.getProjectile() == projectile.bridge$getBukkitEntity()) {
            var spawned = (Projectile) DecorationOps.callsite().invoke(projectile, instance, projectileStack, alreadyShot);
            if (spawned.isRemoved() || !((gr1mly4memes.felt.common.bridge.core.world.entity.projectile.ProjectileSpawnBridge) spawned).bridge$wasSpawnAccepted()) {
                if (livingEntity instanceof net.minecraft.server.level.ServerPlayer player) {
                    ((gr1mly4memes.felt.common.bridge.core.server.level.ServerPlayerBridge) player).bridge$getBukkitEntity().updateInventory();
                }
                return (Projectile) DecorationOps.cancel().invoke();
            }
            return spawned;
        }
        return projectile;
    }

    @Decorate(method = "draw(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ProjectileWeaponItem;useAmmo(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Z)Lnet/minecraft/world/item/ItemStack;"), require = 1)
    private static ItemStack arclight$crossbowLoadAmmo(ItemStack weapon, ItemStack projectile, LivingEntity shooter, boolean forceInfinite) throws Throwable {
        return (ItemStack) DecorationOps.callsite().invoke(weapon, projectile, shooter, forceInfinite || (gr1mly4memes.felt.common.mod.util.FeltCaptures.crossbowNoConsume != null
            && gr1mly4memes.felt.common.mod.util.FeltCaptures.crossbowNoConsume.getFirst() == weapon
            && gr1mly4memes.felt.common.mod.util.FeltCaptures.crossbowNoConsume.getSecond() == shooter));
    }
}
