package gr1mly4memes.felt.bridge.core.server.level;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

public interface DistanceManagerBridge {

    boolean bridge$addTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value);

    boolean bridge$removeTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value);

    boolean bridge$addTicket(long chunkPos, Ticket ticket);

    boolean bridge$removeTicket(long chunkPos, Ticket ticket);

    void bridge$purgeStaleTickets(net.minecraft.server.level.ChunkMap chunkMap);

    void bridge$removeAllTicketsFor(TicketType ticketType, int ticketLevel, Object ticketIdentifier);

    void felt$offerUpdate(ChunkHolder holder);

    void bridge$runQueuedUpdates(net.minecraft.server.level.ChunkMap map);

    default boolean bridge$platform$isTicketForceTick(Ticket ticket) {
        return false;
    }

    default void bridge$mod$addForcedTicket(long chunkPosIn, Ticket ticketIn) {}

    default void bridge$mod$removeForcedTicket(long chunkPosIn, Ticket ticketIn) {}

    java.util.List<Ticket> bridge$getTickets(long chunkPos);

    void bridge$forEachTicket(java.util.function.BiConsumer<net.minecraft.world.level.ChunkPos, Ticket> consumer);
}
