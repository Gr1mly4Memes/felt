package gr1mly4memes.felt.mixin.core.world.item.crafting;

import gr1mly4memes.felt.bridge.core.world.item.crafting.RecipeBridge;
import gr1mly4memes.felt.mod.util.FeltSpecialRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.world.item.crafting.Recipe.class)
public interface RecipeMixin extends RecipeBridge {

    default Recipe toBukkitRecipe(NamespacedKey id) {
        return bridge$toBukkitRecipe(id);
    }

    @Override
    default Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new FeltSpecialRecipe(id, (net.minecraft.world.item.crafting.Recipe<?>) this);
    }
}
