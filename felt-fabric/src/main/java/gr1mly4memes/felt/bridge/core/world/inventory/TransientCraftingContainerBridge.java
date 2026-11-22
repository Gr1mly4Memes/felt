package gr1mly4memes.felt.bridge.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public interface TransientCraftingContainerBridge extends IInventoryBridge {

    void bridge$setOwner(Player owner);

    void bridge$setResultInventory(Container resultInventory);
}
