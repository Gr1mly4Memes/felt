package gr1mly4memes.felt.mod.server.world.inventory;

import net.minecraft.world.inventory.GrindstoneMenu;
import org.bukkit.craftbukkit.inventory.CraftInventoryGrindstone;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;

public class FeltGrindstoneView extends CraftInventoryView<GrindstoneMenu, CraftInventoryGrindstone> {
    public FeltGrindstoneView(HumanEntity player, CraftInventoryGrindstone viewing, GrindstoneMenu container) {
        super(player, viewing, container);
    }
}
