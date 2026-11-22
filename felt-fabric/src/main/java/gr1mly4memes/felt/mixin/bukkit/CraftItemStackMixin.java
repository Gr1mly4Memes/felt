package gr1mly4memes.felt.mixin.bukkit;

import gr1mly4memes.felt.bridge.bukkit.CraftItemStackBridge;
import gr1mly4memes.felt.bridge.core.world.item.ItemStackBridge;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = CraftItemStack.class, remap = false)
public abstract class CraftItemStackMixin extends org.bukkit.inventory.ItemStack implements CraftItemStackBridge {

    // @formatter:off
    @Shadow ItemStack handle;
    @Shadow public abstract Material getType();
    @Shadow public abstract short getDurability();
    @Shadow public abstract boolean hasItemMeta();
    // @formatter:on

    /**
     * @author InitAuther97
     * @reason reduce CraftItemType#bukkitToMinecraft call usage.
     */
    @Overwrite
    public void setType(Material type) {
        if (this.getType() != type) {
            if (type == Material.AIR) {
                this.handle = null;
            } else {
                final var craftType = CraftItemType.bukkitToMinecraft(type);
                if (craftType == null) {
                    this.handle = null;
                } else if (this.handle == null) {
                    this.handle = new net.minecraft.world.item.ItemStack(craftType, 1);
                } else {
                    ((ItemStackBridge)(Object) this.handle).felt$setItem(craftType);
                    if (this.hasItemMeta()) {
                        CraftItemStack.setItemMeta(this.handle, CraftItemStack.getItemMeta(this.handle));
                    }
                }
            }

            this.setData((org.bukkit.material.MaterialData) null);
        }
    }

    @Unique
    private ItemEntity felt$itemEntity;

    @Override
    public void felt$setItemEntity(ItemEntity entity) {
        felt$itemEntity = entity;
    }

    @Override
    public ItemEntity felt$getItemEntity() {
        if (felt$itemEntity != null && this.handle != null && !this.handle.isEmpty()) {
            felt$itemEntity.setItem(this.handle);
        }
        return felt$itemEntity;
    }
}
