package gr1mly4memes.felt.mixin.core.server;

import gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_Fabric implements MinecraftServerBridge {

    @Override
    public void felt$onServerLoad(ServerLevel level) {
        ServerLevelEvents.LOAD.invoker().onLevelLoad((MinecraftServer)(Object)this, level);
    }

    @Override
    public void felt$onServerUnload(ServerLevel level) {
        ServerLevelEvents.UNLOAD.invoker().onLevelUnload((MinecraftServer)(Object)this, level);
    }
}
