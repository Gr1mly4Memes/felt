package gr1mly4memes.felt.mixin.vanilla.server;

import net.minecraft.server.dedicated.DedicatedServer;
import org.spigotmc.AsyncCatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net/minecraft/server/Main$1")
public class Main_ServerShutdownThreadMixin_Vanilla {

    @Redirect(method = "run", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;halt(Z)V"))
    private void felt$shutdown(DedicatedServer instance, boolean b) {
        AsyncCatcher.enabled = false;
        instance.close();
    }
}
