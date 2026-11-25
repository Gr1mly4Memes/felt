package gr1mly4memes.felt;

import com.mojang.authlib.GameProfile;
import gr1mly4memes.felt.mod.util.ConnectionLoginValidation;
import io.papermc.paper.connection.PaperPlayerGameConnection;
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent;
import io.netty.channel.ChannelFutureListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.UpdateInterval;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

final class PaperReconfigurationTest {
    static void verifyTracking(ServerLevel world) {
        var connection = new CapturingConnection();
        var player = setupPlayer(world, connection, "TrackingFixture").player;
        var pig = java.util.Objects.requireNonNull(EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND));
        pig.setPos(15, 90, -29);
        var movement = new ArrayList<Packet<?>>();
        var tracker = new ServerEntity(world, pig, UpdateInterval.NEVER, false, new ServerEntity.Synchronizer() {
            @Override public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
                movement.add(packet);
            }

            @Override public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
                movement.add(packet);
            }

            @Override public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,
                                                                 Predicate<ServerPlayer> filter) {
                if (filter.test(player)) {
                    movement.add(packet);
                }
            }
        });
        pig.setPos(109, 85, -34);
        tracker.addPairing(player);
        var pairing = (ClientboundBundlePacket) connection.sent.getFirst();
        ClientboundAddEntityPacket spawn = null;
        for (var packet : pairing.subPackets()) {
            if (packet instanceof ClientboundAddEntityPacket addEntity) {
                spawn = addEntity;
                break;
            }
        }
        check(spawn != null && spawn.getX() == 109 && spawn.getY() == 85 && spawn.getZ() == -34,
            "A new observer's spawn packet used the old tracker position");
        tracker.sendChanges();
        check(movement.stream().anyMatch(packet -> packet instanceof ClientboundEntityPositionSyncPacket position
            && position.position().endPosition().equals(pig.trackingPosition())),
            "Pairing did not send an absolute movement reset outside the normal update interval");
        movement.clear();
        tracker.sendChanges();
        check(movement.stream().noneMatch(ClientboundEntityPositionSyncPacket.class::isInstance),
            "Movement resync stayed active after its first update");
        var frame = java.util.Objects.requireNonNull(EntityTypes.ITEM_FRAME.create(world, EntitySpawnReason.COMMAND));
        check(frame.getRequiresPrecisePosition(), "Item frames still allow stale relative movement");
        System.out.println("Paper entity tracking: live spawn position, one-shot absolute resync and precise item frames passed");
    }

    static void verify(ServerLevel world, Plugin plugin) {
        var blocked = new CapturingConnection();
        var blockedListener = setupPlayer(world, blocked, "LegacyFixture");
        ConnectionLoginValidation.markLegacyLoginEvent(blocked);
        new PaperPlayerGameConnection(blockedListener).reenterConfiguration();
        check(blocked.sent.isEmpty() && blocked.outbound == null, "Legacy login connection switched to configuration");

        var connection = new CapturingConnection();
        var listener = setupPlayer(world, connection, "ReconfigureFixture");
        var player = listener.player;
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(world.getRespawnData().pos()));
        world.addNewPlayer(player);
        var reported = new AtomicReference<PlayerConnectionReconfigureEvent>();
        var handler = new RegisteredListener(new Listener() {}, (ignored, raw) -> {
            var event = (PlayerConnectionReconfigureEvent) raw;
            check(reported.compareAndSet(null, event), "Configuration acknowledgement dispatched more than one event");
            check(connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl,
                "Reconfigure event was dispatched before configuration took ownership");
        }, EventPriority.NORMAL, plugin, false);
        var handlers = PlayerConnectionReconfigureEvent.getHandlerList();
        handlers.register(handler);
        try {
            new PaperPlayerGameConnection(listener).reenterConfiguration();
            check(player.isRemoved() && connection.sent.contains(ClientboundStartConfigurationPacket.INSTANCE)
                && connection.outbound == ConfigurationProtocols.CLIENTBOUND && reported.get() == null,
                "The Paper API did not take the vanilla play-to-configuration path");
            listener.handleConfigurationAcknowledged(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
            check(connection.inbound == ConfigurationProtocols.SERVERBOUND && reported.get() != null,
                "The acknowledged configuration transition never reached its Paper event");
            reported.get().getConnection().completeReconfiguration();
            var configuration = (ServerConfigurationPacketListenerImpl) connection.getPacketListener();
            check(configuration.currentTask != null, "Completing reconfiguration did not queue a return to the world");
        } finally {
            handlers.unregister(handler);
            if (!player.isRemoved()) {
                world.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            }
        }
        System.out.println("Paper configuration reentry: legacy guard, native stage switch, plugin event and return task passed");
    }

    private static ServerGamePacketListenerImpl setupPlayer(ServerLevel world, CapturingConnection connection, String name) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        var player = new ServerPlayer(world.getServer(), world, profile, ClientInformation.createDefault());
        var listener = new ServerGamePacketListenerImpl(world.getServer(), connection, player, CommonListenerCookie.createInitial(profile, false));
        player.connection = listener;
        connection.listener = listener;
        return listener;
    }

    private static final class CapturingConnection extends Connection {
        private final List<Packet<?>> sent = new ArrayList<>();
        private PacketListener listener;
        private ProtocolInfo<?> inbound;
        private ProtocolInfo<?> outbound;

        CapturingConnection() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override public boolean isConnected() {
            return true;
        }

        @Override public PacketListener getPacketListener() {
            return this.listener;
        }

        @Override public void send(Packet<?> packet, ChannelFutureListener callback, boolean flush) {
            this.sent.add(packet);
        }

        @Override public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocol, T listener) {
            this.inbound = protocol;
            this.listener = listener;
        }

        @Override public void setupOutboundProtocol(ProtocolInfo<?> protocol) {
            this.outbound = protocol;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
