package gr1mly4memes.felt.mod.server.world.inventory;

import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class FeltInventoryView {

    /*
     * Treat all modded containers not having a "bottom" inventory.
     */
    public static InventoryView createInvView(AbstractContainerMenu container) {
        var containerOwner = FeltCaptures.getContainerOwner();
        Inventory viewing = createInv(containerOwner, container);
        return new CraftInventoryView<>(((PlayerBridge) containerOwner).bridge$getBukkitEntity(), viewing, container);
    }

    public static CraftInventory createInv(Player containerOwner, AbstractContainerMenu container) {
        return new FeltModdedMenuInventory(new FeltModdedMenuContainer(container, containerOwner));
    }

    public static SimpleContainer copyOf(SimpleContainer container) {
        var copy = new SimpleContainer(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            copy.items.set(slot, container.items.get(slot).copy());
        }
        return copy;
    }
}
