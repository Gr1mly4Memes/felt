package gr1mly4memes.felt.mixin.core.server.network;

import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "sendChunk",
        at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ClientboundLevelChunkWithLightPacket"))
    private static net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket felt$antiXrayBypass(
            LevelChunk chunk, net.minecraft.world.level.lighting.LevelLightEngine light, java.util.BitSet sky,
            java.util.BitSet block, com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket> original,
            ServerGamePacketListenerImpl connection, ServerLevel level, LevelChunk sentChunk) {
        var controller = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge) level).bridge$chunkPacketBlockController();
        return gr1mly4memes.felt.mod.server.world.FeltAntiXray.withModification(
            controller.shouldModify(connection.getPlayer(), chunk), () -> original.call(chunk, light, sky, block));
    }

    @Decorate(method = "sendChunk(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;)V",
        inject = true, require = 1, allow = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private static void felt$chunkLoad(ServerGamePacketListenerImpl connection, ServerLevel level, LevelChunk chunk) {
        // Preserve the native packet (including mod loader auxiliary light data) and
        // notify before the caller's debug/loader tracking tail, as Paper does.
        if (PlayerChunkLoadEvent.getHandlerList().getRegisteredListeners().length > 0) {
            new PlayerChunkLoadEvent(new CraftChunk(chunk), ((ServerPlayerBridge) connection.getPlayer()).bridge$getBukkitEntity()).callEvent();
        }
    }

    @Decorate(method = "dropChunk(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)V",
        inject = true, require = 1, allow = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void felt$chunkUnload(ServerPlayer player, ChunkPos pos) {
        // This point is reached only when native pending/alive checks sent a
        // forget-chunk packet; removing a pending unsent chunk stays silent.
        if (PlayerChunkUnloadEvent.getHandlerList().getRegisteredListeners().length > 0) {
            CraftPlayer bukkitPlayer = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
            new PlayerChunkUnloadEvent(bukkitPlayer.getWorld().getChunkAt(pos.pack()), bukkitPlayer).callEvent();
        }
    }
}
