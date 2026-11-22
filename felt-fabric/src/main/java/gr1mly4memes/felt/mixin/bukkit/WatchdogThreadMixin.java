package gr1mly4memes.felt.mixin.bukkit;

import gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge;
import gr1mly4memes.felt.mod.server.FeltServer;
import net.minecraft.util.Util;
import org.spigotmc.WatchdogThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WatchdogThread.class, remap = false)
public class WatchdogThreadMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private static void felt$tick(CallbackInfo ci) {
        ((MinecraftServerBridge) FeltServer.getMinecraftServer()).felt$extendNextTickTimeTo(Util.timeSource());
    }
}
