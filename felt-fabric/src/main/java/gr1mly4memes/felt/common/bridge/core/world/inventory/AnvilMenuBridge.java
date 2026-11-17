package gr1mly4memes.felt.common.bridge.core.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface AnvilMenuBridge {

    default boolean bridge$mod$onAnvilChange(AnvilMenu container, @NotNull ItemStack left, @NotNull ItemStack right, Container outputSlot, String name, int baseCost, Player player) {
        return true;
    }

    default boolean bridge$mod$isBookEnchantable(ItemStack a, ItemStack b) {
        return true;
    }

    void arclight$allowZeroCost();

    boolean arclight$isZeroCostAllowed();
}
