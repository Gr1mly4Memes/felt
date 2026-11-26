package gr1mly4memes.felt.mixin.core.recipebook;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.mod.inventory.FeltRecipeBookContents;
import net.minecraft.core.Holder;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
    @Unique private FeltRecipeBookContents felt$componentContents;

    @ModifyExpressionValue(method = "placeRecipe(Lnet/minecraft/recipebook/ServerPlaceRecipe$CraftingMenuAccess;IILjava/util/List;Ljava/util/List;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/crafting/RecipeHolder;ZZ)Lnet/minecraft/world/inventory/RecipeBookMenu$PostPlaceAction;",
        at = @At(value = "NEW", target = "net/minecraft/world/entity/player/StackedItemContents"), require = 1)
    private static StackedItemContents felt$accountComponents(StackedItemContents original, @Local(argsOnly = true) RecipeHolder<?> recipe) {
        var ingredients = recipe.value().placementInfo().ingredients();
        return ingredients.stream().anyMatch(FeltRecipeBookContents::usesComponents)
            ? new FeltRecipeBookContents(ingredients) : original;
    }

    @Inject(method = "tryPlaceRecipe", at = @At("HEAD"))
    private void felt$captureComponents(RecipeHolder<?> recipe, StackedItemContents contents,
                                        CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir) {
        if (contents instanceof FeltRecipeBookContents components) {
            this.felt$componentContents = components;
        }
    }

    @WrapOperation(method = "moveItemToGrid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingCraftingIngredient(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)I"),
        require = 1)
    private int felt$findSelectedStack(Inventory inventory, Holder<Item> ingredient, ItemStack existing, Operation<Integer> original) {
        return this.felt$componentContents == null ? original.call(inventory, ingredient, existing)
            : this.felt$componentContents.findInventorySlot(inventory, ingredient, existing);
    }
}
