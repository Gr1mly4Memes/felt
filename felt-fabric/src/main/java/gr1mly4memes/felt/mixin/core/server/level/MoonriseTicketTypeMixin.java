package gr1mly4memes.felt.mixin.core.server.level;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType;
import gr1mly4memes.felt.bridge.core.server.level.TicketTypeBridge;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import gr1mly4memes.felt.mod.server.world.MoonriseTicketIds;
import net.minecraft.server.level.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.Comparator;

@Mixin(TicketType.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseTicketTypeMixin implements ChunkSystemTicketType<Object> {
    @Unique private final long felt$moonriseId = MoonriseTicketIds.next();
    @Unique private Comparator<Object> felt$identifierComparator;
    @Unique private volatile long[] felt$counterTypes;

    @Override public long moonrise$getId() { return this.felt$moonriseId; }
    @Override public Comparator<Object> moonrise$getIdentifierComparator() { return this.felt$identifierComparator; }
    @Override public void moonrise$setIdentifierComparator(Comparator<Object> comparator) { this.felt$identifierComparator = comparator; }
    @Override public long[] moonrise$getCounterTypes() {
        long[] types = this.felt$counterTypes;
        if (types == null) this.felt$counterTypes = types = PlatformHooks.get().getCounterTypesUncached((TicketType) (Object) this);
        return types;
    }
    @Override public void moonrise$setTimeout(long timeout) { ((TicketTypeBridge) this).bridge$setLifespan(timeout); }
}
