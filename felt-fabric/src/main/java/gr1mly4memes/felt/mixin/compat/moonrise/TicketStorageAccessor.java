package gr1mly4memes.felt.mixin.compat.moonrise;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.function.BiConsumer;

@Mixin(TicketStorage.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.PRESENT)
public interface TicketStorageAccessor {
    @Invoker("forEachTicket")
    void felt$forEachTicket(BiConsumer<ChunkPos, Ticket> consumer);
}
