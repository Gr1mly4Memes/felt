package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;

@Mixin(ThreadedLevelLightEngine.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseThreadedLightEngineMixin implements ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider {
    @Unique private final java.util.concurrent.atomic.AtomicLong chunkWorkCounter = new java.util.concurrent.atomic.AtomicLong();
    @Unique private void queueTaskForSection(final int chunkX, final int chunkY, final int chunkZ,
                                     final java.util.function.Supplier<ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.LightQueue.ChunkTasks> supplier) {
        final ServerLevel world = (ServerLevel)((ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider)(Object)this).starlight$getLightEngine().getWorld();

        final ChunkAccess center = ((ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider)(Object)this).starlight$getLightEngine().getAnyChunkNow(chunkX, chunkZ);
        if (center == null || !center.getPersistedStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.LIGHT)) {
            // do not accept updates in unlit chunks, unless we might be generating a chunk
            return;
        }

        final ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.ServerLightQueue.ServerChunkTasks scheduledTask = (ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.ServerLightQueue.ServerChunkTasks)supplier.get();

        if (scheduledTask == null) {
            // not scheduled
            return;
        }

        if (!scheduledTask.markTicketAdded()) {
            // ticket already added
            return;
        }

        final Long ticketId = Long.valueOf(this.chunkWorkCounter.getAndIncrement());
        final ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().chunkHolderManager.addTicketAtLevel(ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.CHUNK_WORK_TICKET, pos, ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.LIGHT_TICKET_LEVEL, ticketId);

        scheduledTask.queueOrRunTask(() -> {
            ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().chunkHolderManager.removeTicketAtLevel(ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.CHUNK_WORK_TICKET, pos, ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.LIGHT_TICKET_LEVEL, ticketId);
        });
    }

    @Override
    public final int starlight$serverRelightChunks(final java.util.Collection<net.minecraft.world.level.ChunkPos> chunks0,
                                                   final java.util.function.Consumer<net.minecraft.world.level.ChunkPos> chunkLightCallback,
                                                   final java.util.function.IntConsumer onComplete) {
        final java.util.Set<net.minecraft.world.level.ChunkPos> chunks = new java.util.LinkedHashSet<>(chunks0);
        final java.util.Map<net.minecraft.world.level.ChunkPos, Long> ticketIds = new java.util.HashMap<>();
        final ServerLevel world = (ServerLevel)((ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider)(Object)this).starlight$getLightEngine().getWorld();

        if (chunks.isEmpty()) {
            if (onComplete != null) {
                onComplete.accept(0);
            }
            return 0;
        }

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int maxX = Integer.MIN_VALUE;

        for (final java.util.Iterator<net.minecraft.world.level.ChunkPos> iterator = chunks.iterator(); iterator.hasNext();) {
            final ChunkPos pos = iterator.next();

            minX = Math.min(pos.x(), minX);
            minZ = Math.min(pos.z(), minZ);
            maxX = Math.max(pos.x(), maxX);
            maxZ = Math.max(pos.z(), maxZ);

            final Long id = ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.getNextChunkRelightId();
            ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().chunkHolderManager.addTicketAtLevel(ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.CHUNK_RELIGHT, pos, ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.LIGHT_TICKET_LEVEL, id);
            ticketIds.put(pos, id);

            final ChunkAccess chunk = (ChunkAccess)world.getChunkSource().getChunkForLighting(pos.x(), pos.z());
            if (chunk == null || !chunk.isLightCorrect() || !chunk.getPersistedStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.LIGHT)) {
                // cannot relight this chunk
                iterator.remove();
                ticketIds.remove(pos);
                ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().chunkHolderManager.removeTicketAtLevel(ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.CHUNK_RELIGHT, pos, ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.LIGHT_TICKET_LEVEL, id);
                continue;
            }
        }

        ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().radiusAwareScheduler.queueTask(minX, minZ, maxX, maxZ, () -> {
            ((ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider)(Object)this).starlight$getLightEngine().relightChunks(
                chunks,
                (final ChunkPos pos) -> {
                    if (chunkLightCallback != null) {
                        chunkLightCallback.accept(pos);
                    }

                    ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().scheduleChunkTask(pos.x(), pos.z(), () -> {
                        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder chunkHolder = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(
                            pos.x(), pos.z()
                        );

                        if (chunkHolder == null) {
                            return;
                        }

                        final java.util.List<ServerPlayer> players = ((ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemChunkHolder)chunkHolder.vanillaChunkHolder).moonrise$getPlayers(false);

                        if (players.isEmpty()) {
                            return;
                        }

                        final net.minecraft.network.protocol.Packet<?> relightPacket = new net.minecraft.network.protocol.game.ClientboundLightUpdatePacket(
                            pos, (ThreadedLevelLightEngine)(Object)MoonriseThreadedLightEngineMixin.this,
                            null, null
                        );

                        for (final ServerPlayer player : players) {
                            final net.minecraft.server.network.ServerGamePacketListenerImpl conn = player.connection;
                            if (conn != null) {
                                conn.send(relightPacket);
                            }
                        }
                    });
                },
                (final int relight) -> {
                    if (onComplete != null) {
                        onComplete.accept(relight);
                    }

                    for (final java.util.Map.Entry<ChunkPos, Long> entry : ticketIds.entrySet()) {
                        ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler().chunkHolderManager.removeTicketAtLevel(
                            ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.CHUNK_RELIGHT, entry.getKey(),
                            ca.spottedleaf.moonrise.patches.starlight.light.StarLightInterface.LIGHT_TICKET_LEVEL, entry.getValue()
                        );
                    }
                }
            );
        });

        return chunks.size();
    }


    @Overwrite public void checkBlock(BlockPos pos) {
        BlockPos copy = pos.immutable();
        this.queueTaskForSection(copy.getX() >> 4, copy.getY() >> 4, copy.getZ() >> 4, () -> ((ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider)(Object)this).starlight$getLightEngine().blockChange(copy));
    }
    @Overwrite public void updateSectionStatus(SectionPos pos, boolean empty) {
        this.queueTaskForSection(pos.x(), pos.y(), pos.z(), () -> ((ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider)(Object)this).starlight$getLightEngine().sectionChange(pos, empty));
    }
    @Overwrite protected void updateChunkStatus(ChunkPos pos) {}
    @Overwrite public void propagateLightSources(ChunkPos pos) {}
    @Overwrite public void setLightEnabled(ChunkPos pos, boolean enabled) {}
    @Overwrite public void queueSectionData(LightLayer layer, SectionPos pos, net.minecraft.world.level.chunk.DataLayer data) {}
    @Overwrite public void retainData(ChunkPos pos, boolean retain) {}
    @Overwrite public void tryScheduleUpdate() {}
    @Overwrite public java.util.concurrent.CompletableFuture<ChunkAccess> initializeLight(ChunkAccess chunk, boolean lighted) {
        return java.util.concurrent.CompletableFuture.completedFuture(chunk);
    }

}
