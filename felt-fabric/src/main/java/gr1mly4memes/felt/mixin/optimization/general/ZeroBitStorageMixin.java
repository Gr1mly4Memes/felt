package gr1mly4memes.felt.mixin.optimization.general;

import net.minecraft.util.ZeroBitStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Paper - Perf: Optimize SimpleBitStorage. ZeroBitStorage always stores zero, so the
 * bounds/value validation in every accessor is pure overhead on a hot path.
 */
@Mixin(ZeroBitStorage.class)
public abstract class ZeroBitStorageMixin {

    @Redirect(method = {"getAndSet", "set", "fill", "get"},
        at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/Validate;inclusiveBetween(JJJ)V"))
    private void felt$skipValidate(long min, long max, long value) {
        // no-op
    }
}
