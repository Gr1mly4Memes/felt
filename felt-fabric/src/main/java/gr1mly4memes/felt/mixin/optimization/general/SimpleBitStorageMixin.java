package gr1mly4memes.felt.mixin.optimization.general;

import net.minecraft.util.SimpleBitStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Paper - Perf: Optimize SimpleBitStorage. Drops per-access bounds/value validation
 * on a hot path (chunk sections, biomes, heightmaps).
 */
@Mixin(SimpleBitStorage.class)
public abstract class SimpleBitStorageMixin {

    @Redirect(method = {"getAndSet", "set", "fill", "get"},
        at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/Validate;inclusiveBetween(JJJ)V"))
    private void felt$skipValidate(long min, long max, long value) {
        // no-op
    }
}
