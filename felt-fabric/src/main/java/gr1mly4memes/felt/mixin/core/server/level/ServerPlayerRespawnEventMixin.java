package gr1mly4memes.felt.mixin.core.server.level;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRespawnEventMixin implements ServerPlayerBridge {

    @Unique @Nullable private PlayerRespawnEvent felt$completedRespawnEvent;

    @Override
    public void bridge$setPostRespawnEvent(@Nullable PlayerRespawnEvent event) {
        this.felt$completedRespawnEvent = event;
    }

    @Override
    public void bridge$callPostRespawnEvent(ServerPlayer respawnedPlayer, TeleportTransition transition, PlayerRespawnEvent.RespawnReason reason) {
        var respawnEvent = this.felt$completedRespawnEvent;
        this.felt$completedRespawnEvent = null;
        // A disconnected pre-respawn event returns the old native player.
        if (respawnedPlayer == null || respawnedPlayer == (Object) this || transition == null) {
            return;
        }
        // restoreFrom has already rebound the existing CraftPlayer to the new
        // native player. Do not recreate either native player or Bukkit handle.
        new PlayerPostRespawnEvent(
            ((ServerPlayerBridge) respawnedPlayer).bridge$getBukkitEntity(),
            CraftLocation.toBukkit(transition.position(), ((WorldBridge) transition.newLevel()).bridge$getWorld(), transition.yRot(), transition.xRot()),
            respawnEvent != null && respawnEvent.isBedSpawn(),
            respawnEvent != null && respawnEvent.isAnchorSpawn(),
            transition.missingRespawnBlock(),
            respawnEvent == null ? reason : respawnEvent.getRespawnReason()
        ).callEvent();
    }
}
