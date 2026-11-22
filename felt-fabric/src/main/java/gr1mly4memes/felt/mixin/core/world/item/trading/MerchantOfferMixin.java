package gr1mly4memes.felt.mixin.core.world.item.trading;

import gr1mly4memes.felt.bridge.core.world.item.trading.MerchantOfferBridge;
import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.craftbukkit.inventory.CraftMerchantRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin implements MerchantOfferBridge {

    // @formatter:off
    @Shadow public ItemCost baseCostA;
    @Shadow private int demand;
    // @formatter:on

    private CraftMerchantRecipe bukkitHandle;

    @Override
    public void bridge$setBukkitHandle(CraftMerchantRecipe recipe) {
        this.bukkitHandle = recipe;
    }

    public CraftMerchantRecipe asBukkit() {
        return (bukkitHandle == null) ? bukkitHandle = new CraftMerchantRecipe((MerchantOffer) (Object) this) : bukkitHandle;
    }

    @ShadowConstructor
    public void felt$constructor(ItemCost itemCost, Optional<ItemCost> optional, ItemStack itemStack, int i, int j, int k, float f, int l) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void felt$constructor(ItemCost itemCost, Optional<ItemCost> optional, ItemStack itemStack, int i, int j, int k, float f, int l, CraftMerchantRecipe bukkit) {
        felt$constructor(itemCost, optional, itemStack, i, j, k, f, l);
        this.bukkitHandle = bukkit;
    }

    @Override
    public CraftMerchantRecipe bridge$asBukkit() {
        return asBukkit();
    }
}
