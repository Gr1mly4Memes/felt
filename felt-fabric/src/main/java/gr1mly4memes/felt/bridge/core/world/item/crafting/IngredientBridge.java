package gr1mly4memes.felt.bridge.core.world.item.crafting;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IngredientBridge {

    void bridge$setExactChoices(List<ItemStack> exactChoices);

    List<ItemStack> bridge$getExactChoices();

    boolean bridge$isExact();

    void bridge$setStackPredicate(java.util.function.Predicate<ItemStack> predicate);

    java.util.function.Predicate<ItemStack> bridge$getStackPredicate();

    // Vanilla/Fabric have no native custom predicate.
    default boolean bridge$isCustom() {
        return false;
    }
}
