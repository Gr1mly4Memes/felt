package gr1mly4memes.felt.mixin.core.server;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.RegistryLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RegistryLayer.class)
public interface RegistryLayerAccessor {
    @Accessor("STATIC_ACCESS")
    static RegistryAccess.Frozen felt$staticAccess() {
        throw new AssertionError();
    }
}
