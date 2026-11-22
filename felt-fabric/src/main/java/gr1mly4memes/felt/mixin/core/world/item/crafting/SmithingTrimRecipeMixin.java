package gr1mly4memes.felt.mixin.core.world.item.crafting;

import gr1mly4memes.felt.bridge.core.world.item.crafting.RecipeBridge;
import gr1mly4memes.felt.bridge.core.world.item.crafting.SmithingTrimRecipeBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftSmithingTrimRecipe;
import org.bukkit.craftbukkit.inventory.trim.CraftTrimPattern;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SmithingTrimRecipe.class)
public class SmithingTrimRecipeMixin implements RecipeBridge, SmithingTrimRecipeBridge {

    private boolean copyDataComponents = true; // Paper added
    @org.spongepowered.asm.mixin.Unique
    private static org.apache.commons.lang3.tuple.Triple<ItemStack, ItemStack, Holder<TrimPattern>> felt$trimWithoutComponents;

    // @formatter:off
    @Shadow @Final Ingredient template;
    @Shadow @Final Ingredient base;
    @Shadow @Final Ingredient addition;
    @Shadow @Final private Holder<TrimPattern> pattern;
    // @formatter:on

    @Override
    public void bridge$setCopyDataComponents(boolean copy) {
        this.copyDataComponents = copy;
    }

    @Decorate(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/SmithingTrimRecipe;applyTrim(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack felt$trimComponents(ItemStack baseItem, ItemStack materialItem, Holder<TrimPattern> pattern) throws Throwable {
        var previous = felt$trimWithoutComponents;
        felt$trimWithoutComponents = this.copyDataComponents ? null
            : org.apache.commons.lang3.tuple.Triple.of(baseItem, materialItem, pattern);
        try {
            return (ItemStack) DecorationOps.callsite().invoke(baseItem, materialItem, pattern);
        } finally {
            felt$trimWithoutComponents = previous;
        }
    }

    @Decorate(method = "applyTrim(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack felt$copyComponents(ItemStack baseItem, int count, ItemStack nativeBaseItem, ItemStack materialItem, Holder<TrimPattern> pattern) throws Throwable {
        if (felt$trimWithoutComponents != null && felt$trimWithoutComponents.getLeft() == nativeBaseItem
            && felt$trimWithoutComponents.getMiddle() == materialItem && felt$trimWithoutComponents.getRight() == pattern) {
            return new ItemStack(baseItem.getItem(), 1);
        }
        return (ItemStack) DecorationOps.callsite().invoke(baseItem, count);
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new CraftSmithingTrimRecipe(id, CraftRecipe.toChoice(this.template), CraftRecipe.toChoice(this.base), CraftRecipe.toChoice(this.addition), CraftTrimPattern.minecraftHolderToBukkit(this.pattern), this.copyDataComponents);
    }
}
