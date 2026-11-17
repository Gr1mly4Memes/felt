package gr1mly4memes.felt.common.mixin.core.world.item.trading;

import gr1mly4memes.felt.common.bridge.core.world.item.trading.MerchantBridge;
import net.minecraft.world.item.trading.Merchant;
import org.bukkit.craftbukkit.inventory.CraftMerchant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Merchant.class)
public interface MerchantMixin extends MerchantBridge {

    default CraftMerchant getCraftMerchant() {
        return bridge$getCraftMerchant();
    }
}
