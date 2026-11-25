package io.papermc.paper;

import io.papermc.paper.command.PaperSubcommand;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.Strategy;
import org.bukkit.Chunk;
import org.bukkit.World;

public final class FeatureHooks {

    // this includes non-accessible entities
    public static Iterable<Entity> getAllEntities(final net.minecraft.server.level.ServerLevel level) {
        return ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemLevel)level).moonrise$getEntityLookup().getAllMapped();
    }

    public static void setPlayerChunkUnloadDelay(final long ticks) {
        ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.setUnloadDelay(ticks);
    }

    public static void registerPaperCommands(final Map<Set<String>, PaperSubcommand> commands) {
        commands.put(Set.of("fixlight"), new io.papermc.paper.command.subcommands.FixLightCommand());
        commands.put(Set.of("debug", "chunkinfo", "holderinfo"), new io.papermc.paper.command.subcommands.ChunkDebugCommand());
    }

    public static LevelChunkSection createSection(final PalettedContainerFactory palettedContainerFactory, final Level level, final ChunkPos chunkPos, final int chunkSection) {
        return new LevelChunkSection(palettedContainerFactory);
    }

    public static void sendChunkRefreshPackets(final List<ServerPlayer> playersInRange, final LevelChunk chunk) {
        final Map<Boolean, ClientboundLevelChunkWithLightPacket> packets = new HashMap<>();
        final var controller = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge) chunk.getLevel()).bridge$chunkPacketBlockController();
        for (final ServerPlayer player : playersInRange) {
            if (player.connection == null) continue;

            boolean modify = controller.shouldModify(player, chunk);
            player.connection.send(packets.computeIfAbsent(modify, key ->
                gr1mly4memes.felt.mod.server.world.FeltAntiXray.withModification(key, () ->
                    new ClientboundLevelChunkWithLightPacket(chunk, chunk.getLevel().getLightEngine(), null, null))));
        }
    }

    public static PalettedContainer<BlockState> emptyPalettedBlockContainer() {
        return new PalettedContainer<>(Blocks.AIR.defaultBlockState(), Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY));
    }

    public static Set<Long> getSentChunkKeys(final ServerPlayer player) {
        final var loader = ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$getChunkLoader();
        return loader == null ? java.util.Set.of() : LongSets.unmodifiable(new LongOpenHashSet(loader.getSentChunksRaw()));
    }

    public static Set<Chunk> getSentChunks(final ServerPlayer player) {
        final ObjectSet<Chunk> chunks = new ObjectOpenHashSet<>();
        final World world = player.level().getWorld();
        getSentChunkKeys(player).forEach(pos -> {
            final Chunk chunk = world.getChunkAt(pos);
            chunks.add(chunk);
        });
        return ObjectSets.unmodifiable(chunks);
    }

    public static boolean isChunkSent(final ServerPlayer player, final long chunkKey) {
        final var loader = ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$getChunkLoader();
        return loader != null && loader.getSentChunksRaw().contains(chunkKey);
    }

    public static boolean isCollidingWithWorldBorder(final Entity entity) {
        final net.minecraft.world.level.border.WorldBorder border = entity.level().getWorldBorder();
        // Inflate by +EPSILON as collision will just barely place us outside border
        return net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(
            border.getCollisionShape(),
            net.minecraft.world.phys.shapes.Shapes.create(entity.getBoundingBox().inflate(1.0E-7)), net.minecraft.world.phys.shapes.BooleanOp.AND
        ) && border.isInsideCloseToBorder(entity, entity.getBoundingBox());
        // return ca.spottedleaf.moonrise.patches.collisions.CollisionUtil.isCollidingWithBorder(border, entity.getBoundingBox().inflate(ca.spottedleaf.moonrise.patches.collisions.CollisionUtil.COLLISION_EPSILON)) && border.isInsideCloseToBorder(entity, entity.getBoundingBox())
    }

    public static void dumpAllChunkLoadInfo(net.minecraft.server.MinecraftServer server, boolean isLongTimeout) {
        ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler.dumpAllChunkLoadInfo(server, isLongTimeout);
    }

    private static void dumpEntity(final Entity entity) {
    }

    public static org.bukkit.entity.Entity[] getChunkEntities(net.minecraft.server.level.ServerLevel level, int chunkX, int chunkZ) {
        level.getChunk(chunkX, chunkZ);
        var entities = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)level)
            .moonrise$getChunkTaskScheduler().chunkHolderManager.getOrCreateEntityChunk(chunkX, chunkZ, false);
        return entities.getAllEntities().stream().map(Entity::getBukkitEntity)
            .filter(java.util.Objects::nonNull).toArray(org.bukkit.entity.Entity[]::new);
    }

    public static java.util.Collection<org.bukkit.plugin.Plugin> getPluginChunkTickets(net.minecraft.server.level.ServerLevel level,
                                                                                       int x, int z) {
        gr1mly4memes.felt.bridge.core.server.level.DistanceManagerBridge distanceManager = (gr1mly4memes.felt.bridge.core.server.level.DistanceManagerBridge) level.getChunkSource().chunkMap.getDistanceManager();
        List<net.minecraft.server.level.Ticket> tickets = distanceManager.bridge$getTickets(ChunkPos.pack(x, z));

        if (tickets == null) {
            return java.util.Collections.emptyList();
        }

        com.google.common.collect.ImmutableList.Builder<org.bukkit.plugin.Plugin> ret = com.google.common.collect.ImmutableList.builder();
        for (net.minecraft.server.level.Ticket ticket : tickets) {
            if (ticket.getType() == gr1mly4memes.felt.bridge.core.server.level.TicketTypeBridge.bridge$pluginTicket()) {
                ret.add((org.bukkit.plugin.Plugin) ((gr1mly4memes.felt.bridge.core.server.level.TicketBridge) ticket).bridge$getKey());
            }
        }

        return ret.build();
    }

    public static Map<org.bukkit.plugin.Plugin, java.util.Collection<Chunk>> getPluginChunkTickets(net.minecraft.server.level.ServerLevel level) {
        Map<org.bukkit.plugin.Plugin, com.google.common.collect.ImmutableList.Builder<Chunk>> ret = new HashMap<>();
        gr1mly4memes.felt.bridge.core.server.level.DistanceManagerBridge distanceManager = (gr1mly4memes.felt.bridge.core.server.level.DistanceManagerBridge) level.getChunkSource().chunkMap.getDistanceManager();

        distanceManager.bridge$forEachTicket((chunkPos, ticket) -> {
            long chunkKey = chunkPos.pack();
            Chunk chunk = null;
            if (ticket.getType() == gr1mly4memes.felt.bridge.core.server.level.TicketTypeBridge.bridge$pluginTicket()) {
                chunk = level.getWorld().getChunkAt(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey));
                ret.computeIfAbsent((org.bukkit.plugin.Plugin) ((gr1mly4memes.felt.bridge.core.server.level.TicketBridge) ticket).bridge$getKey(), _ -> com.google.common.collect.ImmutableList.builder()).add(chunk);
            }
        });

        return ret.entrySet().stream().collect(com.google.common.collect.ImmutableMap.toImmutableMap(Map.Entry::getKey, (entry) -> entry.getValue().build()));
    }

    public static int getViewDistance(net.minecraft.server.level.ServerLevel level) {
        return ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)level).moonrise$getPlayerChunkLoader().getAPIViewDistance();
    }

    public static int getSimulationDistance(net.minecraft.server.level.ServerLevel level) {
        return ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)level).moonrise$getPlayerChunkLoader().getAPITickDistance();
    }

    public static int getSendViewDistance(net.minecraft.server.level.ServerLevel level) {
        return ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)level).moonrise$getPlayerChunkLoader().getAPISendViewDistance();
    }

    public static void setViewDistance(net.minecraft.server.level.ServerLevel level, int distance) {
        if (distance < 2 || distance > 32) {
            throw new IllegalArgumentException("View distance " + distance + " is out of range of [2, 32]");
        }
        ((gr1mly4memes.felt.bridge.core.server.level.ChunkMapBridge) level.getChunkSource().chunkMap).bridge$setViewDistance(distance);
    }

    public static void setSimulationDistance(net.minecraft.server.level.ServerLevel level, int distance) {
        if (distance < 2 || distance > 32) {
            throw new IllegalArgumentException("Simulation distance " + distance + " is out of range of [2, 32]");
        }
        level.getChunkSource().chunkMap.getDistanceManager().updateSimulationDistance(distance);
    }

    public static void setSendViewDistance(net.minecraft.server.level.ServerLevel level, int distance) {
        if (distance != -1 && (distance < 2 || distance > 32)) throw new IllegalArgumentException("Send view distance must be -1 or between 2 and 32");
        ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)level).moonrise$getPlayerChunkLoader().setSendDistance(distance);
    }

    public static void tickEntityManager(net.minecraft.server.level.ServerLevel level) {
        ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)level).moonrise$getChunkTaskScheduler().executeAllRecentlyQueuedMainThreadTasks();
    }

    public static void closeEntityManager(net.minecraft.server.level.ServerLevel level, boolean save) {
        if ((Object)level instanceof ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel world
            && world.moonrise$getChunkTaskScheduler() != null) return;
        try {
            ((gr1mly4memes.felt.bridge.core.world.level.entity.PersistentEntitySectionManagerBridge) level.entityManager).close(save);
        } catch (final java.io.IOException exception) {
            throw new RuntimeException("Failed to close entity manager", exception);
        }
    }

    public static java.util.concurrent.Executor getWorldgenExecutor() {
        return Runnable::run;
    }

    public static void setViewDistance(ServerPlayer player, int distance) {
        if (distance != -1 && (distance < 2 || distance > 32)) throw new IllegalArgumentException("View distance must be -1 or between 2 and 32");
        ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$getViewDistanceHolder().setLoadViewDistance(distance == -1 ? -1 : distance + 1);
    }

    public static void setSimulationDistance(ServerPlayer player, int distance) {
        if (distance != -1 && (distance < 2 || distance > 32)) throw new IllegalArgumentException("Simulation distance must be -1 or between 2 and 32");
        ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$getViewDistanceHolder().setTickViewDistance(distance);
    }

    public static void setSendViewDistance(ServerPlayer player, int distance) {
        if (distance != -1 && (distance < 2 || distance > 32)) throw new IllegalArgumentException("Send view distance must be -1 or between 2 and 32");
        ((ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer)player).moonrise$getViewDistanceHolder().setSendViewDistance(distance);
    }

    public static io.papermc.paper.world.chunk.ChunkGenerationState getChunkGenerationState(final net.minecraft.server.level.ServerLevel level, final int chunkX, final int chunkZ) {
        return ((gr1mly4memes.felt.bridge.core.server.level.ChunkMapBridge) level.getChunkSource().chunkMap).bridge$getChunkGenerationState(chunkX, chunkZ);
    }

    public static java.util.concurrent.CompletableFuture<io.papermc.paper.world.chunk.ChunkGenerationState> getChunkGenerationStateAsync(final net.minecraft.server.level.ServerLevel level, final int chunkX, final int chunkZ) {
        return ((gr1mly4memes.felt.bridge.core.server.level.ChunkMapBridge) level.getChunkSource().chunkMap).bridge$getChunkGenerationStateAsync(chunkX, chunkZ);
    }

    public static void flushAsyncAppenders() {
    }

}
