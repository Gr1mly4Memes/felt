package gr1mly4memes.felt.mixin.core.world.entity.npc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.world.item.trading.MerchantOfferBridge;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Villager.class)
public abstract class VillagerDiscountMixin {
    @WrapOperation(method = "updateSpecialPrices", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;addToSpecialPriceDiff(I)V"), require = 2, allow = 2)
    private void felt$discountAllowed(MerchantOffer offer, int difference, Operation<Void> original) {
        if (!((MerchantOfferBridge)offer).bridge$ignoreDiscounts()) original.call(offer, difference);
    }
}
