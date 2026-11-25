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
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable
    public static com.mojang.serialization.Codec<MerchantOffer> CODEC;
    @org.spongepowered.asm.mixin.Unique private boolean felt$ignoreDiscounts;
    @Override public boolean bridge$ignoreDiscounts() { return this.felt$ignoreDiscounts; }
    @Override public void bridge$setIgnoreDiscounts(boolean ignoreDiscounts) { this.felt$ignoreDiscounts = ignoreDiscounts; }

    @org.spongepowered.asm.mixin.injection.Inject(method = "<clinit>", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private static void felt$paperOfferCodec(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        CODEC = new gr1mly4memes.felt.mod.util.MerchantOfferCodec(CODEC);
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>(Lnet/minecraft/world/item/trading/MerchantOffer;)V", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void felt$copyDiscountFlag(MerchantOffer offer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.felt$ignoreDiscounts = ((MerchantOfferBridge)offer).bridge$ignoreDiscounts();
    }

    // @formatter:off
    @Shadow public ItemCost baseCostA;
    @Shadow private int demand;
    // @formatter:on

    private CraftMerchantRecipe bukkitHandle;

    @org.spongepowered.asm.mixin.injection.Inject(method = "updateDemand", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void felt$keepDemandNonnegative(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (io.papermc.paper.configuration.GlobalConfiguration.get().misc.preventNegativeVillagerDemand) {
            this.demand = Math.max(0, this.demand);
        }
    }

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
