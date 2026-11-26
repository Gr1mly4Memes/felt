package gr1mly4memes.felt;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu.PostPlaceAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.RecipeChoice;

final class PaperRecipeBookTest {
    static void verify(ServerLevel world) {
        var player = PaperReconfigurationTest.createTestPlayer(world, "RecipeFixture");
        var inventory = player.getInventory();
        var menu = new CraftingMenu(1, inventory);
        var exact = new ItemStack(Items.PAPER);
        exact.set(DataComponents.CUSTOM_NAME, Component.literal("Felt token"));
        exact.set(DataComponents.MAX_STACK_SIZE, 3);
        var wrongToken = exact.copy();
        wrongToken.set(DataComponents.CUSTOM_NAME, Component.literal("Wrong token"));
        var key = new NamespacedKey("felt", "verification_exact_choice");
        var recipe = new org.bukkit.inventory.ShapelessRecipe(key, new org.bukkit.inventory.ItemStack(Material.GLOWSTONE_DUST));
        recipe.addIngredient(RecipeChoice.exactChoice(List.of(CraftItemStack.asBukkitCopy(exact))));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.PAPER));
        var holder = register(world, recipe);
        try {
            var nativeRecipe = (ShapelessRecipe) holder.value();
            check(nativeRecipe.matches(CraftingInput.of(2, 1, List.of(new ItemStack(Items.PAPER), exact)), world),
                "The shapeless recipe failed to resolve overlapping exact and material choices");
            check(!nativeRecipe.matches(CraftingInput.of(2, 1, List.of(new ItemStack(Items.PAPER), wrongToken)), world),
                "The shapeless recipe accepted a different component value");
            var display = nativeRecipe.placementInfo().ingredients().getFirst().display();
            check(display instanceof SlotDisplay.Composite composite
                && composite.contents().getFirst() instanceof SlotDisplay.ItemStackSlotDisplay icon
                && ItemStack.isSameItemSameComponents(icon.stack().create(), exact),
                "The recipe-book display omitted ingredient components");

            inventory.setItem(0, wrongToken.copyWithCount(3));
            inventory.setItem(1, new ItemStack(Items.PAPER, 8));
            check(menu.handlePlacement(false, false, holder, world, inventory) == PostPlaceAction.PLACE_GHOST_RECIPE,
                "The recipe book placed a mismatched ingredient");
            inventory.setItem(2, exact.copyWithCount(3));
            inventory.setItem(3, exact.copyWithCount(3));
            check(menu.handlePlacement(true, false, holder, world, inventory) == PostPlaceAction.NOTHING,
                "The recipe book rejected the matching named item");
            var grid = menu.getInputGridSlots().stream().map(Slot::getItem).filter(stack -> !stack.isEmpty()).toList();
            check(grid.size() == 2 && grid.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, exact)).count() == 1
                && grid.stream().allMatch(stack -> stack.getCount() == 3), "Place-all ignored the exact stack or its custom maximum");
            check(inventory.getItem(0).getCount() == 3 && ItemStack.isSameItemSameComponents(inventory.getItem(0), wrongToken),
                "Recipe-book placement moved the wrong named item");
        } finally {
            Bukkit.removeRecipe(key);
        }

        key = new NamespacedKey("felt", "verification_predicate_choice");
        var predicate = new org.bukkit.inventory.ShapelessRecipe(key, new org.bukkit.inventory.ItemStack(Material.GLOWSTONE_DUST));
        predicate.addIngredient(RecipeChoice.predicateChoice(stack -> stack.getType() == Material.GOLD_NUGGET,
            new org.bukkit.inventory.ItemStack(Material.PAPER)));
        predicate.addIngredient(new RecipeChoice.MaterialChoice(Material.GOLD_NUGGET));
        holder = register(world, predicate);
        try {
            var nativeRecipe = (ShapelessRecipe) holder.value();
            var gold = new ItemStack(Items.GOLD_NUGGET);
            check(nativeRecipe.matches(CraftingInput.of(2, 1, List.of(gold, gold.copy())), world),
                "Shapeless matching used the predicate's display item as its actual choice");
            var otherPlayer = PaperReconfigurationTest.createTestPlayer(world, "PredicateFixture");
            var otherInventory = otherPlayer.getInventory();
            var otherMenu = new CraftingMenu(2, otherInventory);
            otherInventory.setItem(0, gold);
            check(otherMenu.handlePlacement(false, false, holder, world, otherInventory) == PostPlaceAction.PLACE_GHOST_RECIPE,
                "One stack item counted for both the predicate and material ingredient");
            otherInventory.setItem(0, gold.copyWithCount(2));
            check(otherMenu.handlePlacement(false, false, holder, world, otherInventory) == PostPlaceAction.NOTHING
                && otherMenu.getInputGridSlots().stream().filter(slot -> slot.getItem().is(Items.GOLD_NUGGET)).count() == 2,
                "The recipe book could not place an item accepted by both the predicate and material choices");
            otherMenu.getInputGridSlots().forEach(slot -> slot.set(ItemStack.EMPTY));
            var nonStackingGold = gold.copy();
            nonStackingGold.set(DataComponents.MAX_STACK_SIZE, 1);
            for (int slot = 0; slot < 8; ++slot) {
                otherInventory.setItem(slot, nonStackingGold.copy());
            }
            otherInventory.setItem(8, gold.copyWithCount(4));
            check(otherMenu.handlePlacement(true, false, holder, world, otherInventory) == PostPlaceAction.NOTHING
                && otherMenu.getInputGridSlots().stream().filter(slot -> !slot.getItem().isEmpty())
                .allMatch(slot -> slot.getItem().getCount() == 2 && ItemStack.isSameItemSameComponents(slot.getItem(), gold))
                && otherMenu.getInputGridSlots().stream().filter(slot -> !slot.getItem().isEmpty()).count() == 2,
                "Place-all did not select the exact component variants that fit both grid slots");
        } finally {
            Bukkit.removeRecipe(key);
        }
        System.out.println("Paper recipe book: exact displays, component-safe placement, stack limits and overlapping predicate choices passed");
    }

    private static RecipeHolder<?> register(ServerLevel world, org.bukkit.inventory.ShapelessRecipe recipe) {
        check(Bukkit.addRecipe(recipe), "Could not register the Bukkit verification recipe");
        var key = ResourceKey.create(Registries.RECIPE, CraftNamespacedKey.toMinecraft(recipe.getKey()));
        return world.getServer().getRecipeManager().byKey(key).orElseThrow();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
