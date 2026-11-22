package gr1mly4memes.felt.mixin.core.world.item.crafting;

import gr1mly4memes.felt.bridge.core.world.item.crafting.RecipeBridge;
import gr1mly4memes.felt.bridge.core.world.item.crafting.RecipeHolderBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RecipeHolder.class)
public class RecipeHolderMixin<T extends net.minecraft.world.item.crafting.Recipe<?>> implements RecipeHolderBridge {

    @Shadow public native T value();

    @Shadow public native ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> id();

    @Unique private boolean felt$runtimeAddedRecipe;

    @Override
    public boolean bridge$isRuntimeAddedRecipe() {
        return this.felt$runtimeAddedRecipe;
    }

    @Override
    public RecipeHolder<?> bridge$asRuntimeAddedRecipe() {
        var copy = new RecipeHolder<>(this.id(), this.value());
        ((RecipeHolderMixin<T>) (Object) copy).felt$runtimeAddedRecipe = true;
        return copy;
    }

    @Override
    public Recipe bridge$toBukkitRecipe() {
        return toBukkitRecipe();
    }

    public Recipe toBukkitRecipe() {
        return ((RecipeBridge) this.value()).bridge$toBukkitRecipe(CraftNamespacedKey.fromMinecraft(this.id().identifier()));
    }
}
