package gr1mly4memes.felt.mixin.core.server.players;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(net.minecraft.server.players.PlayerList.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonrisePlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void felt$realPlayer(net.minecraft.network.Connection connection, net.minecraft.server.level.ServerPlayer player, net.minecraft.server.network.CommonListenerCookie cookie, CallbackInfo ci) {
        ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$setRealPlayer(true);
    }

}
