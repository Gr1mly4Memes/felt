package gr1mly4memes.felt.common.mixin.core.server.network;

import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLoginPacketListenerImpl.class)
public interface ServerLoginConnectionCloseAccessor {

    @Accessor("state")
    ServerLoginPacketListenerImpl.State arclight$getConnectionCloseState();
}
