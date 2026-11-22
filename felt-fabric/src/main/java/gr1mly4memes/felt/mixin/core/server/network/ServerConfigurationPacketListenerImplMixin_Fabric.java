package gr1mly4memes.felt.mixin.core.server.network;

import gr1mly4memes.felt.mod.server.FeltServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin_Fabric {

    // @formatter:off
    @Shadow public abstract void startConfiguration();
    // @formatter:on

    @Inject(method = "startConfiguration", cancellable = true, at = @At("HEAD"))
    private void felt$runOnMainThread(CallbackInfo ci) {
        if (!FeltServer.isPrimaryThread()) {
            FeltServer.executeOnMainThread(this::startConfiguration);
            ci.cancel();
        }
    }
}
