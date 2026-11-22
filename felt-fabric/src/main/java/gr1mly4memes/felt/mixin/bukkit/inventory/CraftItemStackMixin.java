package gr1mly4memes.felt.mixin.bukkit.inventory;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CraftItemStack.class, remap = false)
public abstract class CraftItemStackMixin {
}
