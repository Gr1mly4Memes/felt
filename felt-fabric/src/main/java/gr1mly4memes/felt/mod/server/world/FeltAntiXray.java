package gr1mly4memes.felt.mod.server.world;

import gr1mly4memes.felt.bridge.core.network.ChunkPacketReadyBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.mixin.core.network.protocol.game.ClientboundLevelChunkPacketDataAccessor;
import io.netty.buffer.Unpooled;
import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.antixray.ChunkPacketBlockControllerAntiXray;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.util.SimpleBitStorage;

/** Uses Paper's engine with a detached global-palette packet snapshot. */
public final class FeltAntiXray {
    private FeltAntiXray() {}

    private static final ThreadLocal<Boolean> MODIFY = ThreadLocal.withInitial(() -> true);

    public static <T> T withModification(boolean modify, java.util.function.Supplier<T> action) {
        boolean previous = MODIFY.get();
        MODIFY.set(modify);
        try { return action.get(); } finally { MODIFY.set(previous); }
    }

    public static void obfuscate(ClientboundLevelChunkWithLightPacket packet, LevelChunk chunk) {
        if (!MODIFY.get()) return;
        var world = (WorldBridge) chunk.getLevel();
        if (world.bridge$paperConfig() == null || !world.bridge$paperConfig().anticheat.antiXray.enabled) return;
        ((ChunkPacketReadyBridge) packet).setReady(false);
        try {
            if (org.bukkit.Bukkit.isPrimaryThread()) prepare(packet, chunk);
            else chunk.getLevel().getServer().execute(() -> prepare(packet, chunk));
        } catch (Throwable failure) {
            fail(packet, failure);
        }
    }

    private static void prepare(ClientboundLevelChunkWithLightPacket packet, LevelChunk chunk) {
        try {
            prepareSnapshot(packet, chunk);
        } catch (Throwable failure) {
            fail(packet, failure);
        }
    }

    private static void prepareSnapshot(ClientboundLevelChunkWithLightPacket packet, LevelChunk chunk) {
        var world = (WorldBridge) chunk.getLevel();
        var controller = world.bridge$chunkPacketBlockController();
        var info = controller.getChunkPacketInfo(chunk);
        if (info == null) {
            ((ChunkPacketReadyBridge) packet).setReady(true);
            return;
        }
        var output = new FriendlyByteBuf(Unpooled.buffer());
        var original = packet.chunkData().getReadBuffer();
        try {
            int bits = 32 - Integer.numberOfLeadingZeros(Block.BLOCK_STATE_REGISTRY.size() - 1);
            for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); sectionIndex++) {
                var section = chunk.getSections()[sectionIndex];
                // Preserve vanilla's block/fluid count header and skip its encoded palettes.
                output.writeShort(original.readShort());
                output.writeShort(original.readShort());
                var states = section.getStates().copy();
                states.read(original);
                var biomes = section.getBiomes().recreate();
                biomes.read(original);
                BlockState[] presets = controller.getPresetBlockStates(chunk.getLevel(), chunk.getPos(),
                    chunk.getMinSectionY() + sectionIndex);
                if (presets == null) {
                    states.write(output);
                } else {
                    // Global IDs include every replacement state without mutating the live chunk palette.
                    output.writeByte(bits);
                    info.setBits(sectionIndex, bits);
                    info.setPalette(sectionIndex, new GlobalPalette<>(Block.BLOCK_STATE_REGISTRY));
                    info.setPresetValues(sectionIndex, presets);
                    info.setIndex(sectionIndex, output.writerIndex());
                    writeGlobalStates(output, states, bits);
                }
                biomes.write(output);
            }
            byte[] buffer = new byte[output.writerIndex()];
            output.getBytes(0, buffer);
            info.setBuffer(buffer);
            info.setChunkPacket(packet);
            ((ClientboundLevelChunkPacketDataAccessor) packet.chunkData()).felt$setBuffer(buffer);
            controller.modifyBlocks(packet, info);
        } finally {
            original.release();
            output.release();
        }
    }

    public static ChunkPacketBlockController createController(net.minecraft.world.level.Level level) {
        return new ChunkPacketBlockControllerAntiXray(level, work -> {
            // Paper captures neighbouring chunks on the server thread before submitting this job.
            var info = (io.papermc.paper.antixray.ChunkPacketInfo<?>) work;
            net.minecraft.util.Util.backgroundExecutor().execute(() -> {
                try { work.run(); } catch (Throwable failure) { fail(info.getChunkPacket(), failure); }
            });
        });
    }

    private static void fail(ClientboundLevelChunkWithLightPacket packet, Throwable failure) {
        ((ChunkPacketReadyBridge) packet).bridge$fail(failure);
        org.slf4j.LoggerFactory.getLogger("Felt").error("Failed to obfuscate chunk packet {}, {}", packet.x(), packet.z(), failure);
    }

    public static void writeGlobalStates(FriendlyByteBuf output,
            net.minecraft.world.level.chunk.PalettedContainer<BlockState> states, int bits) {
        var storage = new SimpleBitStorage(bits, 4096);
        for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            storage.set((y << 8) | (z << 4) | x, Block.BLOCK_STATE_REGISTRY.getId(states.get(x, y, z)));
        }
        output.writeFixedSizeLongArray(storage.getRaw());
    }
}
