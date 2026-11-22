package gr1mly4memes.felt.mixin.core.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMotdMixin implements MinecraftServerBridge {

    @Shadow private String motd;

    // Null until a MOTD is set through Bukkit or MinecraftServer#setMotd; the vanilla MOTD is used until then.
    // Since 26.3, DedicatedServer keeps the MOTD in its settings and overrides getMotd()/setMotd() without
    // calling super, so the setMotd injection below never runs on dedicated servers.
    @Unique private volatile Component felt$motd;

    @Override
    public Component bridge$getMotd() {
        Component motd = this.felt$motd;
        if (motd != null) return motd;
        return LegacyComponentSerializer.legacySection().deserializeOr(((MinecraftServer) (Object) this).getMotd(), Component.empty());
    }

    @Override
    public void bridge$setMotd(Component motd) {
        Component checked = Objects.requireNonNull(motd, "motd");
        this.motd = LegacyComponentSerializer.legacySection().serialize(checked);
        this.felt$motd = checked;
    }

    @Inject(method = "setMotd(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void felt$readLegacyMotd(String motd, CallbackInfo ci) {
        this.felt$motd = LegacyComponentSerializer.legacySection().deserializeOr(this.motd, Component.empty());
    }

    @ModifyReturnValue(method = "getMotd()Ljava/lang/String;", at = @At("RETURN"), require = 1)
    private String felt$legacyMotd(String original) {
        Component motd = this.felt$motd;
        return motd == null ? original : LegacyComponentSerializer.legacySection().serialize(motd);
    }

    @WrapOperation(method = "buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;nullToEmpty(Ljava/lang/String;)Lnet/minecraft/network/chat/Component;"),
        require = 1, expect = 1, allow = 1)
    private net.minecraft.network.chat.Component felt$statusMotd(String legacy, Operation<net.minecraft.network.chat.Component> original) {
        return PaperAdventure.asVanilla(this.bridge$getMotd());
    }
}
