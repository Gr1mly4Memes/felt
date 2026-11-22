package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.item.trading.MerchantBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerPurchaseEventMixin implements MerchantBridge {

    private PlayerPurchaseEvent felt$pendingPurchaseEvent;

    @Override
    public PlayerPurchaseEvent bridge$exchangePurchaseEvent(PlayerPurchaseEvent event) {
        PlayerPurchaseEvent previous = this.felt$pendingPurchaseEvent;
        this.felt$pendingPurchaseEvent = event;
        return previous;
    }

    @Decorate(method = "notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V", inject = true,
        at = @At("HEAD"), require = 1, allow = 1)
    private void felt$capturePurchaseDecision(
            @Local(allocate = "feltPurchaseDecision") PlayerPurchaseEvent event) throws Throwable {
        event = this.felt$pendingPurchaseEvent;
        this.felt$pendingPurchaseEvent = null;
        DecorationOps.blackhole().invoke(event);
    }

    @Decorate(method = "notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;increaseUses()V"), require = 1, allow = 1)
    private void felt$purchaseUses(MerchantOffer offer,
            @Local(allocate = "feltPurchaseDecision") PlayerPurchaseEvent event) throws Throwable {
        if (event == null || event.willIncreaseTradeUses()) {
            DecorationOps.callsite().invoke(offer);
        }
    }

    @Decorate(method = "notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/AbstractVillager;rewardTradeXp(Lnet/minecraft/world/item/trading/MerchantOffer;)V"), require = 1, allow = 1)
    private void felt$purchaseExperience(AbstractVillager villager, MerchantOffer offer,
            @Local(allocate = "feltPurchaseDecision") PlayerPurchaseEvent event) throws Throwable {
        if (event == null || event.isRewardingExp()) {
            DecorationOps.callsite().invoke(villager, offer);
        }
        // Keep the ambient/advancement tail and mod loader's TradeWithVillagerEvent untouched.
    }
}
