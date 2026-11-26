package gr1mly4memes.felt.mod.inventory;

import gr1mly4memes.felt.bridge.core.world.item.crafting.IngredientBridge;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

/** Component-aware accounting for a single recipe-book placement. */
public final class FeltRecipeBookContents extends StackedItemContents {
    private final StackedContents<Holder<Item>> available = new StackedContents<>();
    private final Map<StackKey, Holder<Item>> byStack = new HashMap<>();
    private final Map<Holder<Item>, ItemStack> samples = new IdentityHashMap<>();
    private final List<StackedContents.IngredientInfo<Holder<Item>>> ingredients;

    public FeltRecipeBookContents(List<Ingredient> ingredients) {
        this.ingredients = ingredients.stream().<StackedContents.IngredientInfo<Holder<Item>>>map(ingredient -> {
            boolean componentsMatter = usesComponents(ingredient);
            return holder -> {
                ItemStack sample = this.samples.get(holder);
                return (componentsMatter || Inventory.isUsableForCrafting(sample)) && ingredient.test(sample);
            };
        }).toList();
    }

    public static boolean usesComponents(Ingredient ingredient) {
        var bridge = (IngredientBridge) (Object) ingredient;
        return bridge.bridge$isExact() || bridge.bridge$getStackPredicate() != null;
    }

    @Override
    public void accountSimpleStack(ItemStack stack) {
        // Exact choices may request named, enchanted or damaged items.
        this.accountStack(stack);
    }

    @Override
    public void accountStack(ItemStack stack, int maxCount) {
        if (!stack.isEmpty()) {
            Holder<Item> holder = this.byStack.get(new StackKey(stack));
            if (holder == null) {
                ItemStack sample = stack.copy();
                // The native picker uses holder identity. The native placement limit also reads
                // components from the selected holder, so include stack-specific limits here.
                holder = Holder.direct(sample.getItem(), sample.getComponents());
                this.byStack.put(new StackKey(sample), holder);
                this.samples.put(holder, sample);
            }
            this.available.account(holder, Math.min(maxCount, stack.getCount()));
        }
    }

    @Override
    public boolean canCraft(Recipe<?> recipe, int amount, StackedContents.@Nullable Output<Holder<Item>> output) {
        return !recipe.placementInfo().isImpossibleToPlace() && this.pick(amount, output);
    }

    @Override
    public int getBiggestCraftableStack(Recipe<?> recipe, int maxSize, StackedContents.@Nullable Output<Holder<Item>> output) {
        int minimum = 0;
        int maximum = Math.min(maxSize, this.samples.values().stream().mapToInt(ItemStack::getMaxStackSize).max().orElse(0));
        while (minimum < maximum) {
            int amount = maximum - (maximum - minimum) / 2;
            if (this.pick(amount, null)) {
                minimum = amount;
            } else {
                maximum = amount - 1;
            }
        }
        if (output != null && minimum > 0) {
            this.pick(minimum, output);
        }
        return minimum;
    }

    private boolean pick(int amount, StackedContents.@Nullable Output<Holder<Item>> output) {
        // An ingredient must fit in a single grid slot even when its supply spans
        // several inventory slots. Include that limit when trying alternatives.
        var eligible = amount <= 1 ? this.ingredients : this.ingredients.stream()
            .<StackedContents.IngredientInfo<Holder<Item>>>map(ingredient ->
                holder -> this.samples.get(holder).getMaxStackSize() >= amount && ingredient.acceptsItem(holder))
            .toList();
        return this.available.tryPick(eligible, amount, output);
    }

    @Override
    public void clear() {
        this.available.clear();
        this.byStack.clear();
        this.samples.clear();
    }

    public int findInventorySlot(Inventory inventory, Holder<Item> ingredient, ItemStack existing) {
        ItemStack sample = this.samples.get(ingredient);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, sample)) {
            return Inventory.NOT_FOUND_INDEX;
        }
        var items = inventory.getNonEquipmentItems();
        for (int slot = 0; slot < items.size(); ++slot) {
            ItemStack item = items.get(slot);
            if (!item.isEmpty() && ItemStack.isSameItemSameComponents(item, sample)) {
                return slot;
            }
        }
        return Inventory.NOT_FOUND_INDEX;
    }

    private record StackKey(ItemStack stack) {
        @Override
        public boolean equals(Object other) {
            return other instanceof StackKey key && ItemStack.isSameItemSameComponents(this.stack, key.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stack);
        }
    }
}
