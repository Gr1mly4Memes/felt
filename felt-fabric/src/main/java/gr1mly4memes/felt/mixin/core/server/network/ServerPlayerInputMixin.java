package gr1mly4memes.felt.mixin.core.server.network;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.CraftInput;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayerInputMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerInput", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;setLastClientInput(Lnet/minecraft/world/entity/player/Input;)V"))
    private void felt$dispatchPlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci,
                                          @Share("applySneak") LocalBooleanRef applySneak) {
        var input = packet.input();
        var previous = this.player.getLastClientInput();
        var bukkit = ((ServerPlayerBridge) this.player).bridge$getBukkitEntity();
        if (!input.equals(previous)) {
            new PlayerInputEvent(bukkit, new CraftInput(input)).callEvent();
        }
        if (input.shift() != this.player.getLastClientInput().shift()) {
            applySneak.set(new PlayerToggleSneakEvent(bukkit, input.shift()).callEvent());
        }
    }

    @WrapWithCondition(method = "handlePlayerInput", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V"))
    private boolean felt$applySneakWhenAccepted(ServerPlayer player, boolean sneaking,
                                                @Share("applySneak") LocalBooleanRef applySneak) {
        return applySneak.get();
    }
}
