package gr1mly4memes.felt.common.mixin.core.server.level;

import gr1mly4memes.felt.common.bridge.core.world.server.ChunkHolderBridge;
import gr1mly4memes.felt.common.bridge.core.server.level.DistanceManagerBridge;
import gr1mly4memes.felt.common.bridge.core.server.level.TicketBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;
import java.util.function.Consumer;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements DistanceManagerBridge {

    // @formatter:off
    @Shadow @Final private TicketStorage ticketStorage;
    @Shadow @Final @Mutable private Set<ChunkHolder> chunksToUpdateFutures;
    // @formatter:on

    @Unique
    private Queue<ChunkHolder> arclight$scheduleUpdatingQueue = new LinkedList<>();

    @Override
    public void bridge$purgeStaleTickets(ChunkMap chunkMap) {
        this.ticketStorage.purgeStaleTickets(chunkMap);
    }

    @Override
    public void bridge$runQueuedUpdates(ChunkMap map) {
        final var queue = this.arclight$scheduleUpdatingQueue;
        for (ChunkHolder now = queue.poll(); now != null; now = queue.poll()) {
            ((ChunkHolderBridge) now).bridge$callEventIfUnloading(map);
        }
    }

    @Override
    public void arclight$offerUpdate(ChunkHolder holder) {
        arclight$scheduleUpdatingQueue.add(holder);
    }


    @Decorate(method = "removePlayer", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
    private Object arclight$nullsafeRemovePlayer(Long2ObjectMap<ServerPlayer> instance, long l) throws Throwable {
        Object set = DecorationOps.callsite().invoke(instance, l);
        if (set == null) {
            return DecorationOps.cancel().invoke();
        }
        return set;
    }

    @Unique
    private static Ticket arclight$keyedTicket(TicketType type, int level, Object value) {
        Ticket ticket = new Ticket(type, level);
        ((TicketBridge) (Object) ticket).bridge$setKey(value);
        return ticket;
    }

    public boolean addRegionTicketAtDistance(TicketType type, ChunkPos pos, int level, Object value) {
        return this.addTicket(pos.pack(), arclight$keyedTicket(type, 33 - level, value));
    }

    public boolean removeRegionTicketAtDistance(TicketType type, ChunkPos pos, int level, Object value) {
        return this.removeTicket(pos.pack(), arclight$keyedTicket(type, 33 - level, value));
    }

    public boolean addTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value) {
        return this.addTicket(pos.pack(), arclight$keyedTicket(type, level, value));
    }

    public boolean removeTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value) {
        return this.removeTicket(pos.pack(), arclight$keyedTicket(type, level, value));
    }

    @Override
    public boolean bridge$addTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value) {
        return addTicketAtLevel(type, pos, level, value);
    }

    @Override
    public boolean bridge$removeTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value) {
        return removeTicketAtLevel(type, pos, level, value);
    }

    boolean removeTicket(long chunkPosIn, Ticket ticketIn) {
        boolean removed = this.ticketStorage.removeTicket(chunkPosIn, ticketIn);
        if (removed && bridge$platform$isTicketForceTick(ticketIn)) {
            this.bridge$mod$removeForcedTicket(chunkPosIn, ticketIn);
        }
        return removed;
    }

    @Override
    public boolean bridge$removeTicket(long chunkPos, Ticket ticket) {
        return removeTicket(chunkPos, ticket);
    }

    boolean addTicket(long chunkPosIn, Ticket ticketIn) {
        boolean added = this.ticketStorage.addTicket(chunkPosIn, ticketIn);
        if (added && bridge$platform$isTicketForceTick(ticketIn)) {
            this.bridge$mod$addForcedTicket(chunkPosIn, ticketIn);
        }
        return added;
    }

    @Override
    public boolean bridge$addTicket(long chunkPos, Ticket ticket) {
        return addTicket(chunkPos, ticket);
    }

    public void removeAllTicketsFor(TicketType ticketType, int ticketLevel, Object ticketIdentifier) {
        this.ticketStorage.removeTicketIf((ticket, chunkPos) -> ticket.getType() == ticketType && ticket.getTicketLevel() == ticketLevel
            && Objects.equals(((TicketBridge) (Object) ticket).bridge$getKey(), ticketIdentifier), null);
    }

    @Override
    public void bridge$removeAllTicketsFor(TicketType ticketType, int ticketLevel, Object ticketIdentifier) {
        removeAllTicketsFor(ticketType, ticketLevel, ticketIdentifier);
    }
}
