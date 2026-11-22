package gr1mly4memes.felt.mixin.core.server;

import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spigotmc.AsyncCatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/server/Main$1")
public class Main_ServerShutdownThreadMixin {

    @Decorate(method = "run", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;halt(Z)V"))
    private void felt$shutdown(DedicatedServer instance, boolean b) throws Throwable {
        AsyncCatcher.enabled = false;
        DecorationOps.callsite().invoke(instance, instance.isRunning() && b);
    }
}