package gr1mly4memes.felt.common.mixin.core.world.inventory;

import gr1mly4memes.felt.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CraftingContainer.class)
public interface CraftingContainerMixin extends IInventoryBridge {

    @Override
    default RecipeHolder<?> getCurrentRecipe() {
        return null;
    }

    @Override
    default void setCurrentRecipe(RecipeHolder<?> recipe) {
    }
}
