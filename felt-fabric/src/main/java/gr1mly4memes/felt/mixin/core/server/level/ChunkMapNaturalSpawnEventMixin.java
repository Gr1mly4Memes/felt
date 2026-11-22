package gr1mly4memes.felt.mixin.core.server.level;

import com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.bridge.core.server.level.NaturalSpawnSelectionBridge;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ChunkMap.class)
public abstract class ChunkMapNaturalSpawnEventMixin implements NaturalSpawnSelectionBridge {

    @Shadow @Final private ServerLevel level;

    @Unique private final Map<ServerPlayer, PlayerNaturallySpawnCreaturesEvent> felt$naturalSpawnEvents = new IdentityHashMap<>();
    @Unique private boolean felt$collectingNaturalSpawnChunks;

    @Override
    public void bridge$clearNaturalSpawnEvents() {
        this.felt$naturalSpawnEvents.clear();
    }

    @Override
    public void bridge$prepareNaturalSpawnEvents() {
        for (ServerPlayer player : this.level.players()) {
            var bukkitPlayer = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
            int radius = Math.min(((WorldBridge) this.level).bridge$spigotConfig().mobSpawnRange, bukkitPlayer.getViewDistance());
            radius = Math.min(radius, 8);
            var event = new PlayerNaturallySpawnCreaturesEvent(bukkitPlayer, (byte) radius);
            this.felt$naturalSpawnEvents.put(player, event);
            event.callEvent();
        }
    }

    @WrapMethod(method = "collectSpawningChunks(Ljava/util/List;)V", require = 1, allow = 1)
    private void felt$naturalSpawnSelection(List<LevelChunk> chunks, Operation<Void> original) {
        boolean previous = this.felt$collectingNaturalSpawnChunks;
        this.felt$collectingNaturalSpawnChunks = true;
        try {
            // Preserve the native candidate iterator and mod loader force-spawn
            // ticket branch. Only the ordinary per-player predicate is edited.
            original.call(chunks);
        } finally {
            this.felt$collectingNaturalSpawnChunks = previous;
        }
    }

    @WrapMethod(method = "playerIsCloseEnoughForSpawning(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)Z",
        require = 1, allow = 1)
    private boolean felt$naturalSpawnPlayer(ServerPlayer player, ChunkPos pos, Operation<Boolean> original) {
        boolean closeEnough = original.call(player, pos);
        if (!this.felt$collectingNaturalSpawnChunks) {
            return closeEnough;
        }
        var event = this.felt$naturalSpawnEvents.get(player);
        return closeEnough && event != null && !event.isCancelled();
    }

    @ModifyConstant(method = "playerIsCloseEnoughForSpawning(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)Z",
        constant = @Constant(doubleValue = 16384.0), require = 1, allow = 1)
    private double felt$naturalSpawnRadius(double original, ServerPlayer player, ChunkPos pos) {
        if (!this.felt$collectingNaturalSpawnChunks) {
            return original;
        }
        var event = this.felt$naturalSpawnEvents.get(player);
        if (event == null || event.isCancelled()) {
            return 0.0;
        }
        int blockRadius = event.getSpawnRadius() << 4;
        return (double) (blockRadius * blockRadius);
    }
}
