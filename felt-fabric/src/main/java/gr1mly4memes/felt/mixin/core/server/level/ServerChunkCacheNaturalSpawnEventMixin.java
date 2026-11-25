package gr1mly4memes.felt.mixin.core.server.level;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.bridge.core.server.level.NaturalSpawnSelectionBridge;
import gr1mly4memes.felt.bridge.core.world.server.ServerChunkProviderBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheNaturalSpawnEventMixin {

    @Shadow @Final public ChunkMap chunkMap;
    @Shadow public boolean spawnEnemies;

    @Shadow @Final public net.minecraft.server.level.ServerLevel level;

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;createState(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;Lnet/minecraft/world/level/LocalMobCapCalculator;)Lnet/minecraft/world/level/NaturalSpawner$SpawnState;"))
    private net.minecraft.world.level.NaturalSpawner.SpawnState felt$resetPlayerMobCounts(int chunks, net.minecraft.server.level.ServerLevel world,
            net.minecraft.world.level.NaturalSpawner.ChunkGetter getter, net.minecraft.world.level.LocalMobCapCalculator localCaps,
            Operation<net.minecraft.world.level.NaturalSpawner.SpawnState> original) {
        if ((this.spawnEnemies || ((ServerChunkProviderBridge)this).bridge$getSpawnFriendlies())
            && gr1mly4memes.felt.mod.server.world.FeltMobSpawning.perPlayer(world)) gr1mly4memes.felt.mod.server.world.FeltMobSpawning.reset(world);
        return original.call(chunks, world, getter, gr1mly4memes.felt.mod.server.world.FeltMobSpawning.perPlayer(world) ? null : localCaps);
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;shuffle(Ljava/util/List;Lnet/minecraft/util/RandomSource;)V"))
    private void felt$perPlayerSpawnOrder(java.util.List<?> chunks, net.minecraft.util.RandomSource random, Operation<Void> original) {
        if (!gr1mly4memes.felt.mod.server.world.FeltMobSpawning.perPlayer(this.level)) original.call(chunks, random);
    }

    @WrapMethod(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V", require = 1, allow = 1)
    private void felt$naturalSpawnEventLifetime(ProfilerFiller profiler, Operation<Void> original) {
        var selection = (NaturalSpawnSelectionBridge) this.chunkMap;
        selection.bridge$clearNaturalSpawnEvents();
        try {
            original.call(profiler);
        } finally {
            selection.bridge$clearNaturalSpawnEvents();
        }
    }

    @Decorate(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;", ordinal = 1),
        require = 1, allow = 1)
    private Object felt$prepareNaturalSpawnEvents(GameRules rules, GameRule<?> rule,
                                                     @Local(ordinal = 0) boolean doMobSpawning) throws Throwable {
        Object tickSpeed = DecorationOps.callsite().invoke(rules, rule);
        // RANDOM_TICK_SPEED is the second read, after doMobSpawning has been
        // settled and before persistent/category calculations on both loaders.
        // The existing no-player redirect owns only the first rules read.
        if (doMobSpawning && (this.spawnEnemies || ((ServerChunkProviderBridge) this).bridge$getSpawnFriendlies())) {
            ((NaturalSpawnSelectionBridge) this.chunkMap).bridge$prepareNaturalSpawnEvents();
        }
        return tickSpeed;
    }
}
