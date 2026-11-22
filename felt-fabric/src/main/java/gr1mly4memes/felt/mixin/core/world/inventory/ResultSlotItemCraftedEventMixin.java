package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import io.papermc.paper.event.inventory.ItemCraftedEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ResultSlot.class)
public abstract class ResultSlotItemCraftedEventMixin {

    @Shadow @Final private Player player;

    @Decorate(method = "onQuickCraft(Lnet/minecraft/world/item/ItemStack;I)V", inject = true,
        at = @At("RETURN"), require = 1, allow = 1)
    private void felt$quickCraftedItem(ItemStack picked, int count) {
        // Keep native achievements and the loader crafting hook ahead of this notification.
        if (!picked.isEmpty()) {
            new ItemCraftedEvent((org.bukkit.entity.Player) ((PlayerBridge) this.player).bridge$getBukkitEntity(),
                CraftItemStack.asBukkitCopy(picked)).callEvent();
        }
    }

    @Decorate(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", inject = true,
        at = @At("HEAD"), require = 1, allow = 1)
    private void felt$takenCraftedItem(Player player, ItemStack carried) {
        if (!carried.isEmpty()) {
            new ItemCraftedEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
                CraftItemStack.asBukkitCopy(carried)).callEvent();
        }
    }
}
