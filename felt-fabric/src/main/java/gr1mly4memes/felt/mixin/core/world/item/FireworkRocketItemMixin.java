package gr1mly4memes.felt.mixin.core.world.item;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.bridge.core.world.entity.projectile.ProjectileSpawnBridge;
import gr1mly4memes.felt.bridge.core.world.entity.SpawningEntityBridge;
import gr1mly4memes.felt.bridge.core.world.inventory.AbstractContainerMenuBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {

    // These native continuations must run after successful admission, in Paper's order.
    // WrapOperation lets them escape into invocation-local shares without replacing the calls.
    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;dropAllLeashConnections(Lnet/minecraft/world/entity/player/Player;)Z"), require = 1, allow = 1)
    private boolean felt$deferBoostLeashes(Player player, Player source, Operation<Boolean> original,
                                             @Share("feltBoostLeashes") LocalRef<BooleanSupplier> leashes) {
        leashes.set(() -> original.call(player, source));
        // Enter the old branch only to capture the sound, not to play it before the event.
        return true;
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), require = 1, allow = 1)
    private void felt$deferBoostSound(Level level, Entity excluded, Entity source, SoundEvent soundEvent,
                                         SoundSource soundSource, float volume, float pitch, Operation<Void> original,
                                         @Share("feltBoostSound") LocalRef<Runnable> sound) {
        sound.set(() -> original.call(level, excluded, source, soundEvent, soundSource, volume, pitch));
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/projectile/Projectile;"), require = 1, allow = 1)
    private Projectile felt$elytraBoostEvent(Projectile projectile, ServerLevel serverLevel, ItemStack stack,
                                                Operation<Projectile> original, Level level, Player player, InteractionHand hand,
                                                @Share("feltBoostEvent") LocalRef<PlayerElytraBoostEvent> boostEvent,
                                                @Cancellable CallbackInfoReturnable<InteractionResult> cir) {
        ((SpawningEntityBridge) projectile).bridge$setSpawningEntity(player.getUUID());
        var event = new PlayerElytraBoostEvent((org.bukkit.entity.Player) player.bridge$getBukkitEntity(),
            CraftItemStack.asCraftMirror(stack), (org.bukkit.entity.Firework) projectile.bridge$getBukkitEntity(),
            CraftEquipmentSlot.getHand(hand));
        boostEvent.set(event);
        if (!event.callEvent()) {
            felt$resyncBoostItem(player, hand);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return projectile;
        }
        // Retain the original helper and all loader/mod preparation, admission and spawn effects.
        Projectile spawned = original.call(projectile, serverLevel, stack);
        if (!((ProjectileSpawnBridge) spawned).bridge$wasSpawnAccepted()) {
            felt$resyncBoostItem(player, hand);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
        return spawned;
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"), require = 1, allow = 1)
    private void felt$deferBoostConsumption(ItemStack stack, int amount, LivingEntity entity, Operation<Void> original,
                                               Level level, Player player, InteractionHand hand,
                                               @Share("feltBoostEvent") LocalRef<PlayerElytraBoostEvent> boostEvent,
                                               @Share("feltBoostConsume") LocalRef<Runnable> consume) {
        PlayerElytraBoostEvent event = boostEvent.get();
        consume.set(() -> {
            if (event.shouldConsume() && !player.hasInfiniteMaterials()) {
                original.call(stack, amount, entity);
            } else {
                felt$resyncBoostItem(player, hand);
            }
        });
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;)V"), require = 1, allow = 1)
    private void felt$finishBoost(Player player, Stat<?> stat, Operation<Void> original,
                                     @Share("feltBoostLeashes") LocalRef<BooleanSupplier> leashes,
                                     @Share("feltBoostSound") LocalRef<Runnable> sound,
                                     @Share("feltBoostConsume") LocalRef<Runnable> consume) {
        original.call(player, stat);
        if (leashes.get().getAsBoolean()) {
            sound.get().run();
        }
        consume.get().run();
    }

    @Unique
    private static void felt$resyncBoostItem(Player player, InteractionHand hand) {
        if (player.containerMenu instanceof InventoryMenu) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
            ((AbstractContainerMenuBridge) player.containerMenu).bridge$forceSlot(player.getInventory(), slot);
        } else {
            player.containerMenu.sendAllDataToRemote();
            // Native menu synchronization omits the offhand; Paper sends it separately.
            ((AbstractContainerMenuBridge) player.inventoryMenu).bridge$forceSlot(player.getInventory(), Inventory.SLOT_OFFHAND);
        }
    }
}
