package gr1mly4memes.felt.mixin.vanilla.server.dedicated;

import gr1mly4memes.felt.mod.util.FeltTerminalReader;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/server/dedicated/DedicatedServer$1")
public class DedicatedServer_ServerInputThreadMixin_Vanilla {

    @Shadow(aliases = {"field_13822", "f_139700_", "this$0"}) private DedicatedServer outerThis;

    @Inject(method = "run", cancellable = true, at = @At("HEAD"))
    private void felt$terminalConsole(CallbackInfo ci) {
        if (FeltTerminalReader.handleCommands(outerThis)) {
            ci.cancel();
        }
    }
}
