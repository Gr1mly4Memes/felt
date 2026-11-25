package gr1mly4memes.felt.mixin.core.server.level;

import ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicket;
import ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType;
import gr1mly4memes.felt.bridge.core.server.level.TicketBridge;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Ticket.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseTicketMixin implements ChunkSystemTicket<Object> {
    @Shadow private long ticksLeft;
    @Shadow public abstract TicketType getType();
    @Shadow public abstract int getTicketLevel();

    @Override public long moonrise$getRemoveDelay() { return this.ticksLeft; }
    @Override public void moonrise$setRemoveDelay(long delay) { this.ticksLeft = delay; }
    @Override public Object moonrise$getIdentifier() { return ((TicketBridge) this).bridge$getKey(); }
    @Override public void moonrise$setIdentifier(Object identifier) {
        var type = (ChunkSystemTicketType<?>) (Object) this.getType();
        if ((identifier == null) != (type.moonrise$getIdentifierComparator() == null)) {
            throw new IllegalStateException("Nullability of identifier should match nullability of comparator");
        }
        ((TicketBridge) this).bridge$setKey(identifier);
    }
    @Override @SuppressWarnings("unchecked")
    public int compareTo(Ticket other) {
        int level = Integer.compare(this.getTicketLevel(), other.getTicketLevel());
        if (level != 0) return level;
        var type = (ChunkSystemTicketType<Object>) (Object) this.getType();
        var otherType = (ChunkSystemTicketType<?>) (Object) other.getType();
        int order = Long.compare(type.moonrise$getId(), otherType.moonrise$getId());
        if (order != 0) return order;
        var comparator = type.moonrise$getIdentifierComparator();
        return comparator == null ? 0 : comparator.compare(this.moonrise$getIdentifier(), ((TicketBridge) (Object) other).bridge$getKey());
    }
}
