package gr1mly4memes.felt.mixin.core.network.protocol.game;

import gr1mly4memes.felt.bridge.core.network.ChunkPacketReadyBridge;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Adds Paper's asynchronous anti-xray readiness state to the vanilla packet. */
@Mixin(ClientboundLevelChunkWithLightPacket.class)
public class ClientboundLevelChunkWithLightPacketMixin implements ChunkPacketReadyBridge {

    @Unique private final gr1mly4memes.felt.mod.util.AsyncPacketState felt$state = new gr1mly4memes.felt.mod.util.AsyncPacketState();

    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/lighting/LevelLightEngine;Ljava/util/BitSet;Ljava/util/BitSet;)V",
        at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void felt$obfuscate(net.minecraft.world.level.chunk.LevelChunk chunk,
            net.minecraft.world.level.lighting.LevelLightEngine light, java.util.BitSet sky, java.util.BitSet block,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        gr1mly4memes.felt.mod.server.world.FeltAntiXray.obfuscate((ClientboundLevelChunkWithLightPacket) (Object) this, chunk);
    }

    @Override
    public boolean isReady() {
        return this.felt$state.isReady();
    }

    @Override
    public void setReady(boolean ready) {
        this.felt$state.setReady(ready);
    }

    @Override
    public java.util.concurrent.CompletableFuture<Void> bridge$completion() {
        return this.felt$state.completion();
    }

    @Override
    public void bridge$fail(Throwable failure) {
        this.felt$state.fail(failure);
    }
}
