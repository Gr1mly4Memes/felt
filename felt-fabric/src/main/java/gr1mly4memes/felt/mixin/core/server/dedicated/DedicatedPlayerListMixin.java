package gr1mly4memes.felt.mixin.core.server.dedicated;

import gr1mly4memes.felt.bridge.bukkit.CraftServerBridge;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * Fix adventure-platform-fabric replacing List<ServerPlayer> causing
 * Bukkit#getOnlinePlayers returns nothing
 */
@Mixin(DedicatedPlayerList.class)
public class DedicatedPlayerListMixin {
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void felt$afterSuper(CallbackInfo ci) {
        ((CraftServerBridge)Bukkit.getServer()).bridge$setPlayerList((DedicatedPlayerList)(Object) this);
    }
}
