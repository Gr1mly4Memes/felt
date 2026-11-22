package gr1mly4memes.felt.mixin.core.world.level;

import gr1mly4memes.felt.bridge.core.server.level.TicketBridge;
import gr1mly4memes.felt.bridge.core.world.level.TicketStorageBridge;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;
import java.util.function.BiConsumer;

@Mixin(TicketStorage.class)
public abstract class TicketStorageMixin implements TicketStorageBridge {

    @Shadow
    abstract void forEachTicket(BiConsumer<ChunkPos, Ticket> consumer);

    /**
     * @author IzzelAliz
     * @reason CraftBukkit includes the plugin key in ticket identity for both add and remove.
     */
    @Overwrite
    private static boolean isTicketSameTypeAndLevel(Ticket ticket, Ticket other) {
        return other.getType() == ticket.getType()
            && other.getTicketLevel() == ticket.getTicketLevel()
            && Objects.equals(((TicketBridge) (Object) ticket).bridge$getKey(),
                ((TicketBridge) (Object) other).bridge$getKey());
    }

    @Override
    public void bridge$forEachTicket(final BiConsumer<ChunkPos, Ticket> consumer) {
        this.forEachTicket(consumer);
    }
}
