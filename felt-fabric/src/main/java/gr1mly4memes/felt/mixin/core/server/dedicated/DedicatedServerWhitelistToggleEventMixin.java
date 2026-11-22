package gr1mly4memes.felt.mixin.core.server.dedicated;

import com.destroystokyo.paper.event.server.WhitelistToggleEvent;
import gr1mly4memes.felttools.mixin.Decorate;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerWhitelistToggleEventMixin {

    @Decorate(method = "setUsingWhitelist(Z)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void felt$whitelistToggled(boolean enabled) {
        new WhitelistToggleEvent(enabled).callEvent();
    }
}
