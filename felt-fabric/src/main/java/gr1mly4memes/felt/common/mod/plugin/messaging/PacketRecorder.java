package gr1mly4memes.felt.common.mod.plugin.messaging;

import gr1mly4memes.felt.common.mod.FeltConstants;
import gr1mly4memes.felt.common.mod.server.FeltServer;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.util.Util;
import net.minecraft.resources.Identifier;

import java.util.stream.Collectors;

public class PacketRecorder {
    private final Object2IntArrayMap<Identifier> unknown = new Object2IntArrayMap<>();
    private long lastUpdate = Util.getMillis();

    public PacketRecorder() {
        unknown.defaultReturnValue(0);
    }

    public void recordUnknown(Identifier id) {
        if (id == null) {
            FeltServer.LOGGER.debug("Received packet with null id. This should never happen.");
            return;
        }
        int num = unknown.getInt(id);
        unknown.put(id, num + 1);
    }

    public void update() {
        long now = Util.getMillis();
        if (Math.abs(now - lastUpdate) > FeltConstants.PACKET_RECORDER_PERIOD_SEC *1000) {
            consumeAndLog();
            lastUpdate = now;
        }
    }

    public void consumeAndLog() {
        String unknowns = unknown.object2IntEntrySet().stream()
                .map(it -> it.getKey().toString() + '(' + it.getIntValue() + ')')
                .collect(Collectors.joining(", ", "unknown=[", "];"));
        unknown.clear();

        FeltServer.LOGGER.debug("Packet error statistics: {}", unknowns);
    }
}
