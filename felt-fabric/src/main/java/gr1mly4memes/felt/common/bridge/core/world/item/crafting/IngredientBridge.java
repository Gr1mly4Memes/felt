package gr1mly4memes.felt.common.bridge.core.world.item.crafting;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IngredientBridge {

    void bridge$setExactChoices(List<ItemStack> exactChoices);

    List<ItemStack> bridge$getExactChoices();

    boolean bridge$isExact();

    // Vanilla/Fabric have no native custom predicate; mod loader overrides this bridge.
    default boolean bridge$isCustom() {
        return false;
    }
}
