package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge;
import gr1mly4memes.felt.bridge.core.server.level.ChunkMapBridge;
import gr1mly4memes.felt.bridge.core.world.server.ServerChunkProviderBridge;
import gr1mly4memes.felt.mod.server.FeltServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.server.level.ServerChunkCache$MainThreadExecutor")
public abstract class ServerChunkCache_MainThreadExecutorMixin extends BlockableEventLoop<Runnable> {

    // @formatter:off
    @Shadow(aliases = {"this$0", "f_8491_", "field_18810"}, remap = false) @Final private ServerChunkCache outer;
    // @formatter:on

    protected ServerChunkCache_MainThreadExecutorMixin(String nameIn) {
        super(nameIn, false);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean pollTask() {
        try {
            if ((Object) outer.chunkMap.level instanceof ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel world
                && world.moonrise$getChunkTaskScheduler() != null) {
                world.moonrise$getChunkTaskScheduler().chunkHolderManager.processTicketUpdates();
                return super.pollTask() | world.moonrise$getChunkTaskScheduler().executeMainThreadTask();
            }
            if (((ServerChunkProviderBridge) outer).bridge$tickDistanceManager()) {
                return true;
            } else {
                ((ServerChunkProviderBridge) outer).bridge$getLightManager().tryScheduleUpdate();
                return super.pollTask();
            }
        } finally {
            ((ChunkMapBridge) outer.chunkMap).bridge$getCallbackExecutor().run();
            // InitAuther97: drain queued tasks when the server thread is waiting for chunks
            // This carries the AsyncCatcher to ensure no timeout unexpectedly.
            ((MinecraftServerBridge) FeltServer.getMinecraftServer()).bridge$drainQueuedTasks();
        }
    }
}
