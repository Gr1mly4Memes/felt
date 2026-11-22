package gr1mly4memes.felt.mod.util;

import gr1mly4memes.felt.bridge.core.world.item.crafting.RecipeManagerBridge;
import gr1mly4memes.felt.mod.server.FeltServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FeltSpecialRecipe extends CraftComplexRecipe {

    private final Recipe<?> recipe;

    public FeltSpecialRecipe(NamespacedKey id, Recipe<?> recipe) {
        super(id, new org.bukkit.inventory.ItemStack(Material.AIR), null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull org.bukkit.inventory.ItemStack getResult() {
        ItemStack result = ItemStack.EMPTY;
        List<RecipeDisplay> displays = this.recipe.display();
        if (!displays.isEmpty()) {
            result = displays.getFirst().result().resolveForFirstStack(SlotDisplayContext.fromLevel(FeltServer.getMinecraftServer().overworld()));
        }
        return CraftItemStack.asCraftMirror(result);
    }

    @Override
    public void addToRecipeManager() {
        ((RecipeManagerBridge) FeltServer.getMinecraftServer().getRecipeManager()).bridge$addRecipe(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, CraftNamespacedKey.toMinecraft(this.getKey())), this.recipe));
    }
}
