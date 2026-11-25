package gr1mly4memes.felt.mod.server.world;

import ca.spottedleaf.moonrise.common.misc.NearbyPlayers;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import gr1mly4memes.felt.bridge.core.entity.InitialSpawnReasonBridge;
import gr1mly4memes.felt.bridge.core.server.level.PlayerMobCountsBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.bridge.core.world.spawner.WorldEntitySpawnerBridge.EntityDensityManagerBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.util.CraftSpawnCategory;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/** Paper's per-player caps and cancellation backoff, using Moonrise's tick-distance player map. */
public final class FeltMobSpawning {
    private FeltMobSpawning() {}
    private static final ThreadLocal<Budget> ACTIVE_BUDGET = new ThreadLocal<>();
    private static final class Budget {
        int remaining;
        Budget(int remaining) { this.remaining = remaining; }
    }
    public static boolean perPlayer(ServerLevel level) {
        return ((WorldBridge)level).bridge$paperConfig().entities.spawning.perPlayerMobSpawns;
    }
    public static boolean countsForSpawning(ServerLevel level, Entity entity) {
        if (((WorldBridge)level).bridge$paperConfig().entities.spawning.countAllMobsForSpawning) return true;
        var reason = ((InitialSpawnReasonBridge)entity).bridge$initialSpawnReason();
        return reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GEN;
    }
    public static void reset(ServerLevel level) {
        for (ServerPlayer player : level.players()) reset(player);
    }
    public static void reset(ServerPlayer player) {
        var counts = (PlayerMobCountsBridge)player;
        java.util.Arrays.fill(counts.bridge$mobCounts(), 0);
        int[] backoff = counts.bridge$mobBackoffCounts();
        for (int i = 0; i < backoff.length; ++i) backoff[i] = Math.max(0, backoff[i] - 1);
    }
    public static int count(ServerPlayer player, MobCategory category) {
        var counts = (PlayerMobCountsBridge)player;
        return counts.bridge$mobCounts()[category.ordinal()] + counts.bridge$mobBackoffCounts()[category.ordinal()];
    }
    public static void track(ServerLevel level, Entity entity) {
        if (perPlayer(level) && (level.getChunkSource().spawnEnemies
            || ((gr1mly4memes.felt.bridge.core.world.server.ServerChunkProviderBridge)level.getChunkSource()).bridge$getSpawnFriendlies()))
            update(level, entity.chunkPosition(), entity.getType().getCategory(), false);
    }
    public static void failure(ServerLevel level, ChunkPos pos, MobCategory category) {
        if (perPlayer(level)) update(level, pos, category, true);
    }
    private static void update(ServerLevel level, ChunkPos pos, MobCategory category, boolean failure) {
        var players = ((ChunkSystemServerLevel)level).moonrise$getNearbyPlayers().getPlayers(pos, NearbyPlayers.NearbyMapType.TICK_VIEW_DISTANCE);
        if (players == null) return;
        for (int i = 0, size = players.size(); i < size; ++i) {
            var counts = (PlayerMobCountsBridge)players.getRawDataUnchecked()[i];
            ++(failure ? counts.bridge$mobBackoffCounts() : counts.bridge$mobCounts())[category.ordinal()];
        }
    }
    public static int remaining(ServerLevel level, ChunkPos pos, MobCategory category) {
        int limit = category.getMaxInstancesPerChunk();
        var bukkitCategory = CraftSpawnCategory.toBukkit(category);
        if (CraftSpawnCategory.isValidForLimits(bukkitCategory)) limit = ((WorldBridge)level).bridge$getWorld().getSpawnLimit(bukkitCategory);
        var players = ((ChunkSystemServerLevel)level).moonrise$getNearbyPlayers().getPlayers(pos, NearbyPlayers.NearbyMapType.TICK_VIEW_DISTANCE);
        if (players == null) return 0;
        int remaining = Integer.MAX_VALUE;
        for (int i = 0, size = players.size(); i < size; ++i) remaining = Math.min(remaining, limit - count(players.getRawDataUnchecked()[i], category));
        return remaining == Integer.MAX_VALUE ? 0 : remaining;
    }
    public static void spawnForChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state, java.util.List<MobCategory> categories) {
        var density = (EntityDensityManagerBridge)state;
        for (MobCategory category : categories) {
            if (!perPlayer(level)) {
                if (density.bridge$canSpawnLocal(category, chunk.getPos())) NaturalSpawner.spawnCategoryForChunk(category, level, chunk, density::bridge$canSpawn, density::bridge$updateDensity);
                continue;
            }
            int remaining = remaining(level, chunk.getPos(), category);
            if (remaining <= 0) continue;
            Budget previous = ACTIVE_BUDGET.get();
            Budget budget = new Budget(remaining);
            ACTIVE_BUDGET.set(budget);
            try {
                NaturalSpawner.spawnCategoryForChunk(category, level, chunk,
                    (type, world, pos, access) -> budget.remaining > 0 && density.bridge$canSpawn(type, world, pos, access),
                    (mob, access) -> {
                        density.bridge$updateDensity(mob, access);
                        track(level, mob);
                        --budget.remaining;
                    });
            } finally {
                if (previous == null) ACTIVE_BUDGET.remove(); else ACTIVE_BUDGET.set(previous);
            }
        }
    }
    public static boolean budgetExhausted() {
        Budget budget = ACTIVE_BUDGET.get();
        return budget != null && budget.remaining <= 0;
    }
}
