package gr1mly4memes.felt.mod.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import gr1mly4memes.felt.bridge.core.world.item.trading.MerchantOfferBridge;
import net.minecraft.world.item.trading.MerchantOffer;

/** Adds Paper's server-only flag without changing vanilla's item-cost codec. */
public record MerchantOfferCodec(Codec<MerchantOffer> original) implements Codec<MerchantOffer> {
    @Override public <T> DataResult<Pair<MerchantOffer, T>> decode(DynamicOps<T> ops, T input) {
        boolean ignore = ops.get(input, "Paper.IgnoreDiscounts").flatMap(ops::getBooleanValue).result().orElse(false);
        return this.original.decode(ops, input).map(pair -> {
            ((MerchantOfferBridge)pair.getFirst()).bridge$setIgnoreDiscounts(ignore);
            return pair;
        });
    }
    @Override public <T> DataResult<T> encode(MerchantOffer offer, DynamicOps<T> ops, T prefix) {
        var encoded = this.original.encode(offer, ops, prefix);
        if (!((MerchantOfferBridge)offer).bridge$ignoreDiscounts()) return encoded;
        return encoded.flatMap(value -> ops.mergeToMap(value, ops.createString("Paper.IgnoreDiscounts"), ops.createBoolean(true)));
    }
}
