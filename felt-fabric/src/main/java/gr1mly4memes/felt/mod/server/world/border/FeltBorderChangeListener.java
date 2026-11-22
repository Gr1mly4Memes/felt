package gr1mly4memes.felt.mod.server.world.border;

import gr1mly4memes.felt.bridge.core.world.level.border.WorldBorderBridge;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

import java.util.function.Function;

public class FeltBorderChangeListener implements BorderChangeListener {

    public static final FeltBorderChangeListener INSTANCE = new FeltBorderChangeListener();

    public static BorderChangeListener typed() {
        return INSTANCE;
    }

    @Override
    public void onSetSize(WorldBorder border, double newSize) {
        felt$broadcastToDimension(border, ClientboundSetBorderSizePacket::new);
    }

    @Override
    public void onLerpSize(WorldBorder border, double fromSize, double targetSize, long ticks, long gameTime) {
        felt$broadcastToDimension(border, ClientboundSetBorderLerpSizePacket::new);
    }

    @Override
    public void onSetCenter(WorldBorder border, double x, double z) {
        felt$broadcastToDimension(border, ClientboundSetBorderCenterPacket::new);
    }

    @Override
    public void onSetWarningTime(WorldBorder border, int time) {
        felt$broadcastToDimension(border, ClientboundSetBorderWarningDelayPacket::new);
    }

    @Override
    public void onSetWarningBlocks(WorldBorder border, int blocks) {
        felt$broadcastToDimension(border, ClientboundSetBorderWarningDistancePacket::new);
    }

    @Override
    public void onSetDamagePerBlock(WorldBorder border, double damagePerBlock) {

    }

    @Override
    public void onSetSafeZone(WorldBorder border, double safeZone) {

    }

    private void felt$broadcastToDimension(WorldBorder border, Function<WorldBorder, Packet<?>> packet) {
        final var level = ((WorldBorderBridge) border).bridge$getWorld();
        level.getServer().getPlayerList().broadcastAll(packet.apply(border), level.dimension());
    }
}
