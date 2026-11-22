package gr1mly4memes.felt.bridge.core.world.level;

import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.ChunkPos;

import java.util.function.BiConsumer;

public interface TicketStorageBridge {

    void bridge$forEachTicket(BiConsumer<ChunkPos, Ticket> consumer);
}
