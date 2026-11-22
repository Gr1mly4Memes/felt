package gr1mly4memes.felt.mixin.core.world.item;

import gr1mly4memes.felt.bridge.core.world.item.ItemStackBridge;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackBridge {

    // @formatter:off
    @Shadow @Final @Mutable PatchedDataComponentMap components;
    @Shadow @Final @Mutable @Deprecated @Nullable private Holder<Item> item;
    // @formatter:on

    @Override
    public void restorePatch(DataComponentPatch datacomponentpatch) {
        this.felt$restorePatch(datacomponentpatch);
    }

    @Override
    public void felt$restorePatch(DataComponentPatch datacomponentpatch) {
        this.components.restorePatch(datacomponentpatch);
    }

    @Override
    @Deprecated
    public void setItem(Item item) {
        this.felt$setItem(item);
    }

    @Deprecated
    public void setItem(@Nullable Holder<Item> item) {
        if (item == null || (Object) this == ItemStack.EMPTY) {
            return;
        }
        if (this.bridge$canSetItem(item)) {
            this.item = item;
            DataComponentPatch patch = ((ItemStack) (Object) this).getComponentsPatch();
            this.components = new PatchedDataComponentMap(item.components());
            this.components.applyPatch(patch);
        }
    }

    @Override
    public void felt$setItem(Item item) {
        this.setItem(item == null ? null : item.builtInRegistryHolder());
    }

    @Override
    public org.bukkit.inventory.ItemStack asBukkitCopy() {
        return CraftItemStack.asBukkitCopy((ItemStack) (Object) this);
    }

    @Override
    public org.bukkit.inventory.ItemStack asBukkitMirror() {
        return CraftItemStack.asCraftMirror((ItemStack) (Object) this);
    }

    @Override
    public void bridge$hurtAndBreak(int amount, LivingEntity owner, @Nullable EquipmentSlot slot, boolean force) {
        if (owner.level() instanceof ServerLevel level) {
            FeltCaptures.withItemDamage((ItemStack) (Object) this, owner, force,
                () -> this.bridge$platform$hurtAndBreak(amount, level, owner, brokenItem -> {
                    if (slot != null) {
                        owner.onEquippedItemBroken(brokenItem, slot);
                    }
                }));
        }
    }

    public void hurtAndBreak(int amount, net.minecraft.world.entity.LivingEntity owner, net.minecraft.world.entity.EquipmentSlot slot, boolean force) {
        this.bridge$hurtAndBreak(amount, owner, slot, force);
    }

}
