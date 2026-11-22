package gr1mly4memes.felt.mixin.core.server.rcon;

import gr1mly4memes.felt.bridge.core.server.dedicated.DedicatedServerBridge;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.thread.RconClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Socket;

@Mixin(RconClient.class)
public class RconClientMixin {
    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.server.rcon.RconConsoleSource rconConsoleSource; // Paper added

    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>(Lnet/minecraft/server/ServerInterface;Ljava/lang/String;Ljava/net/Socket;)V", at = @At("RETURN"))
    private void felt$createSource(ServerInterface server, String password, Socket socket, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (server instanceof net.minecraft.server.MinecraftServer minecraftServer && server instanceof DedicatedServerBridge) {
            this.rconConsoleSource = new net.minecraft.server.rcon.RconConsoleSource(minecraftServer);
            ((gr1mly4memes.felt.bridge.core.server.rcon.RconConsoleSourceBridge) this.rconConsoleSource).bridge$setSocketAddress(socket.getRemoteSocketAddress());
        }
    }

    @Redirect(method = "run()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;runCommand(Ljava/lang/String;)Ljava/lang/String;"))
    private String felt$runCommandWithAddress(ServerInterface server, String command) {
        if (server instanceof DedicatedServerBridge bridge && this.rconConsoleSource != null) {
            return bridge.bridge$runRconCommand(this.rconConsoleSource, command);
        }
        return server.runCommand(command);
    }
}
