package gr1mly4memes.felt.mod.server.world;

import gr1mly4memes.felt.bridge.core.world.entity.player.SpawningInfluenceBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;

public final class FeltSpawningInfluence {
    private FeltSpawningInfluence() {}
    public static boolean affects(Player player) { return ((SpawningInfluenceBridge)player).bridge$affectsSpawning(); }
    public static boolean eligible(Player player) { return !player.isSpectator() && player.isAlive() && affects(player); }
    public static boolean nearbyAlive(EntityGetter level, double x, double y, double z, double range) {
        for (Player player : level.players()) if (eligible(player) && (range < 0 || player.distanceToSqr(x, y, z) < range * range)) return true;
        return false;
    }
    public static Player nearest(EntityGetter level, double x, double y, double z, double range, boolean excludeCreative) {
        var predicate = excludeCreative ? EntitySelector.NO_CREATIVE_OR_SPECTATOR : EntitySelector.NO_SPECTATORS;
        return level.getNearestPlayer(x, y, z, range, predicate.and(entity -> affects((Player)entity)));
    }
    public static Player nearestForDespawn(EntityGetter level, Entity source, double range) {
        return level.getNearestPlayer(source.getX(), source.getY(), source.getZ(), range, entity -> entity instanceof Player player && eligible(player));
    }
}
