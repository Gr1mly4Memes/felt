package gr1mly4memes.felt.mod.server.world;

import ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType;
import net.minecraft.server.level.TicketType;

public final class FeltChunkTickets {
    private FeltChunkTickets() {}
    public static final TicketType POST_TELEPORT = ChunkSystemTicketType.create("post_teleport", null, 5L);
}
