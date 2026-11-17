package gr1mly4memes.felt.common.mod.server.world.inventory;

import net.minecraft.world.inventory.AnvilMenu;
import org.bukkit.craftbukkit.inventory.view.CraftAnvilView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.AnvilInventory;

public class FeltAnvilView extends CraftAnvilView {
    public FeltAnvilView(HumanEntity player, AnvilInventory viewing, AnvilMenu container) {
        super(player, viewing, container);
    }
}
