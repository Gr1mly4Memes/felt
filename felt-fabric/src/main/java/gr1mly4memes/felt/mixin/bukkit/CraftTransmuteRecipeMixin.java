package gr1mly4memes.felt.mixin.bukkit;

import org.bukkit.craftbukkit.inventory.CraftTransmuteRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftTransmuteRecipe.class, remap = false)
public abstract class CraftTransmuteRecipeMixin {

    @Inject(method = "addToRecipeManager", at = @At("HEAD"))
    private void felt$validateMaterialCount(CallbackInfo ci) {
        CraftTransmuteRecipe recipe = (CraftTransmuteRecipe) (Object) this;
        felt$validateBounds(recipe.getMinimumMaterialCount(), recipe.getMaximumMaterialCount());
    }

    private static void felt$validateBounds(int min, int max) {
        if (min < 1 || max > 8 || min > max) {
            throw new IllegalArgumentException("Transmute material count must satisfy 1 <= minimum <= maximum <= 8");
        }
    }
}
