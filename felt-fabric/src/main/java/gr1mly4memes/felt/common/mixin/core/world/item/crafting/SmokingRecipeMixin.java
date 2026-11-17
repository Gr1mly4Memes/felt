package gr1mly4memes.felt.common.mixin.core.world.item.crafting;

import gr1mly4memes.felt.common.bridge.core.world.item.crafting.RecipeBridge;
import gr1mly4memes.felt.common.mod.util.FeltSpecialRecipe;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe.CommonInfo;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmokingRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftSmokingRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SmokingRecipe.class)
public abstract class SmokingRecipeMixin extends AbstractCookingRecipe implements RecipeBridge {

    public SmokingRecipeMixin(CommonInfo commonInfo, CookingBookInfo bookInfo, Ingredient input, ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, input, result, experience, cookingTime);
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        var output = this.result().create();
        if (output.isEmpty()) {
            return new FeltSpecialRecipe(id, this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(output);
        CraftSmokingRecipe recipe = new CraftSmokingRecipe(id, result, CraftRecipe.toChoice(this.input()), this.experience(), this.cookingTime());
        recipe.setGroup(this.group());
        recipe.setCategory(CraftRecipe.getCategory(this.category()));
        return recipe;
    }
}
