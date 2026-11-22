package gr1mly4memes.felt.mixin.core.world;

import gr1mly4memes.felt.bridge.core.world.IInventoryBridge;
import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(SimpleContainer.class)
public abstract class SimpleContainerMixin implements Container, IInventoryBridge {

    // @formatter:off
    @Shadow @Final public NonNullList<ItemStack> items;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;
    protected InventoryHolder bukkitOwner;

    @ShadowConstructor
    public void felt$constructor(int numSlots) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void felt$constructor(int numSlots, InventoryHolder owner) {
        this.felt$constructor(numSlots);
        this.bukkitOwner = owner;
    }

    @Override
    public List<ItemStack> getContents() {
        return this.items;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public InventoryHolder getOwner() {
        return bukkitOwner;
    }

    @Override
    public void setOwner(InventoryHolder owner) {
        this.bukkitOwner = owner;
    }

    @Override
    public int getMaxStackSize() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public RecipeHolder<?> getCurrentRecipe() {
        return null;
    }

    @Override
    public void setCurrentRecipe(RecipeHolder<?> recipe) {

    }
}
