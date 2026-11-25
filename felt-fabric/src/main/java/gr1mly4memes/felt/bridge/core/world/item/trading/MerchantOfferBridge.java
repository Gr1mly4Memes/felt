package gr1mly4memes.felt.bridge.core.world.item.trading;

import org.bukkit.craftbukkit.inventory.CraftMerchantRecipe;

public interface MerchantOfferBridge {
    boolean bridge$ignoreDiscounts();
    void bridge$setIgnoreDiscounts(boolean ignoreDiscounts);

    CraftMerchantRecipe bridge$asBukkit();

    void bridge$setBukkitHandle(CraftMerchantRecipe recipe);

    default CraftMerchantRecipe asBukkit() {
        return this.bridge$asBukkit();
    }

}
