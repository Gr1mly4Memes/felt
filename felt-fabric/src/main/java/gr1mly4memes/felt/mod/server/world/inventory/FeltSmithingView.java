package gr1mly4memes.felt.mod.server.world.inventory;

import net.minecraft.world.inventory.SmithingMenu;
import org.bukkit.craftbukkit.inventory.CraftInventorySmithing;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;

public class FeltSmithingView extends CraftInventoryView<SmithingMenu, CraftInventorySmithing> {
    public FeltSmithingView(HumanEntity player, CraftInventorySmithing viewing, SmithingMenu container) {
        super(player, viewing, container);
    }
}
