package gr1mly4memes.felt.common.mod.server.world.inventory;

import com.google.common.base.Preconditions;
import gr1mly4memes.felt.common.mod.server.FeltServer;
import net.minecraft.world.Container;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.inventory.ItemStack;

public class FeltModdedMenuInventory extends CraftInventory {
    public FeltModdedMenuInventory(Container inventory) {
        super(inventory);
    }

    @Override
    public void setContents(ItemStack[] items) {
        FeltServer.LOGGER.debug("Overriding content for a modded container menu inventory");
        super.setContents(items);
    }

    @Override
    public void clear() {
        FeltServer.LOGGER.debug("Clearing everything for a modded container menu inventory");
        super.clear();
    }
}
