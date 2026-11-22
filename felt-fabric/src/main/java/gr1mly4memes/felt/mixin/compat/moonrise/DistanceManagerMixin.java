package gr1mly4memes.felt.mixin.compat.moonrise;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.bridge.core.server.level.TicketBridge;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(DistanceManager.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.PRESENT)
public abstract class DistanceManagerMixin {
    @Shadow @Final private TicketStorage ticketStorage;

    @WrapMethod(method = "removeAllTicketsFor", remap = false)
    private void felt$removeTickets(TicketType ticketType, int ticketLevel, Object ticketIdentifier, Operation<Void> original) {
        List<com.mojang.datafixers.util.Pair<ChunkPos, Ticket>> matches = new ArrayList<>();
        ((gr1mly4memes.felt.mixin.compat.moonrise.TicketStorageAccessor) this.ticketStorage).felt$forEachTicket((pos, ticket) -> {
            if (ticket.getType() == ticketType && ticket.getTicketLevel() == ticketLevel
                && Objects.equals(((TicketBridge) (Object) ticket).bridge$getKey(), ticketIdentifier)) {
                matches.add(com.mojang.datafixers.util.Pair.of(pos, ticket));
            }
        });
        for (var match : matches) {
            this.ticketStorage.removeTicket(match.getFirst().pack(), match.getSecond());
        }
    }

}
