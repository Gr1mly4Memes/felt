package gr1mly4memes.felt.mixin.core.util;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import net.minecraft.world.entity.PostSpawnProcessor;

@Mixin(SpawnUtil.class)
public abstract class SpawnUtilPreCreatureSpawnEventMixin {

    @Decorate(method = "trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;Z)Ljava/util/Optional;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"),
        require = 1, allow = 1)
    private static Entity felt$preCreatureSpawn(EntityType<?> type, ServerLevel level, PostSpawnProcessor<?> config,
            BlockPos searchPos, EntitySpawnReason spawnReason, boolean tryMoveDown, boolean movedUp,
            EntityType<?> entityType, EntitySpawnReason reason, ServerLevel world, BlockPos start) throws Throwable {
        var context = FeltCaptures.preCreatureSpawn;
        var event = new PreCreatureSpawnEvent(CraftLocation.toBukkit(start, ((WorldBridge) level).bridge$getWorld()),
            CraftEntityType.minecraftToBukkit(type), FeltCaptures.getPreCreatureSpawnReason());
        if (!event.callEvent()) {
            if (event.shouldAbortSpawn()) {
                if (context != null && context.getSecond() != null) {
                    context.getSecond().run();
                }
                // The enclosing host returns Optional, not the create call's Entity.
                return (Entity) DecorationOps.cancel().invoke(Optional.empty());
            }
            // Paper breaks the attempt loop even for a non-aborting veto.
            throw DecorationOps.jumpToLoopEnd();
        }
        return (Entity) DecorationOps.callsite().invoke(type, level, config, searchPos, spawnReason, tryMoveDown, movedUp);
    }

    @Decorate(method = "trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;Z)Ljava/util/Optional;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"),
        require = 1, allow = 1)
    private static void felt$spawnReason(ServerLevel level, Entity entity) throws Throwable {
        var world = (WorldBridge) level;
        var previous = world.bridge$getAddEntityReason();
        world.bridge$pushAddEntityReason(FeltCaptures.getPreCreatureSpawnReason());
        try {
            DecorationOps.callsite().invoke(level, entity);
        } finally {
            world.bridge$pushAddEntityReason(previous);
        }
        if (entity.isRemoved()) {
            DecorationOps.cancel().invoke(Optional.empty());
            return;
        }
    }
}
