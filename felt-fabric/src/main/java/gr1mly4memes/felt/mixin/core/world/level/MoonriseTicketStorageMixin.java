package gr1mly4memes.felt.mixin.core.world.level;

import ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketStorage;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.ChunkPos;


@Mixin(TicketStorage.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseTicketStorageMixin implements ChunkSystemTicketStorage {
    @Unique private ChunkMap felt$owner;
    @Override public ChunkMap moonrise$getChunkMap() { return this.felt$owner; }
    @Override public void moonrise$setChunkMap(ChunkMap map) { this.felt$owner = map; }

    @Shadow @Final private it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<java.util.List<Ticket>> deactivatedTickets;
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager felt$manager() {
        if (this.felt$owner == null) return null;
        var scheduler = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.felt$owner.level).moonrise$getChunkTaskScheduler();
        return scheduler == null ? null : scheduler.chunkHolderManager;
    }
    @Inject(method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), cancellable = true)
    private void felt$add(long key, Ticket ticket, CallbackInfoReturnable<Boolean> cir) {
        if (this.felt$manager() == null) return;
        boolean added = this.felt$manager().addTicketAtLevel(ticket.getType(), key, ticket.getTicketLevel(), ((ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicket<?>)ticket).moonrise$getIdentifier());
        ((TicketStorage)(Object)this).setDirty(); cir.setReturnValue(added);
    }
    @Inject(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), cancellable = true)
    private void felt$remove(long key, Ticket ticket, CallbackInfoReturnable<Boolean> cir) {
        if (this.felt$manager() == null) return;
        boolean removed = this.felt$manager().removeTicketAtLevel(ticket.getType(), key, ticket.getTicketLevel(), ((ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicket<?>)ticket).moonrise$getIdentifier());
        if (removed) ((TicketStorage)(Object)this).setDirty(); cir.setReturnValue(removed);
    }
    @Inject(method = "getTickets", at = @At("HEAD"), cancellable = true)
    private void felt$tickets(long key, CallbackInfoReturnable<java.util.List<Ticket>> cir) { if (this.felt$manager() != null) cir.setReturnValue(this.felt$manager().getTicketsAt(ChunkPos.unpack(key).x(), ChunkPos.unpack(key).z())); }
    @Inject(method = "hasTickets", at = @At("HEAD"), cancellable = true)
    private void felt$has(CallbackInfoReturnable<Boolean> cir) { if (this.felt$manager() != null) cir.setReturnValue(this.felt$manager().hasTickets()); }
    @Inject(method = "shouldKeepDimensionActive", at = @At("HEAD"), cancellable = true)
    private void felt$active(CallbackInfoReturnable<Boolean> cir) { if (this.felt$manager() != null) { var counters = this.felt$manager().getTicketCounters(ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType.COUNTER_TYPE_KEEP_DIMENSION_ACTIVE); cir.setReturnValue(counters != null && !counters.isEmpty()); } }
    @Inject(method = "purgeStaleTickets", at = @At("HEAD"), cancellable = true)
    private void felt$purge(ChunkMap map, CallbackInfo ci) { if (this.felt$manager() != null) { this.felt$manager().tick(); ((TicketStorage)(Object)this).setDirty(); ci.cancel(); } }
    @Inject(method = "deactivateTicketsOnClosing", at = @At("HEAD"), cancellable = true)
    private void felt$deactivate(CallbackInfo ci) { if (this.felt$manager() != null) ci.cancel(); }
    @Inject(method = "forEachTicket(Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), cancellable = true)
    private void felt$forEach(java.util.function.BiConsumer<ChunkPos, Ticket> consumer, CallbackInfo ci) {
        if (this.felt$manager() == null) return;
        for (var entry : this.felt$manager().getTicketsCopy().long2ObjectEntrySet()) for (Ticket ticket : entry.getValue()) consumer.accept(ChunkPos.unpack(entry.getLongKey()), ticket);
        for (var entry : this.deactivatedTickets.long2ObjectEntrySet()) for (Ticket ticket : entry.getValue()) consumer.accept(ChunkPos.unpack(entry.getLongKey()), ticket);
        ci.cancel();
    }
    @Inject(method = "getForceLoadedChunks", at = @At("HEAD"), cancellable = true)
    private void felt$forced(CallbackInfoReturnable<it.unimi.dsi.fastutil.longs.LongSet> cir) {
        if (this.felt$manager() == null) return;
        var counters = this.felt$manager().getTicketCounters(ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType.COUNTER_TYPE_FORCED);
        var ret = counters == null ? new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet() : new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet(counters.size(), counters.getLoadFactor());
        if (counters != null) { var iterator = counters.keyIterator(); while (iterator.hasNext()) ret.add(iterator.nextLong()); }
        cir.setReturnValue(ret);
    }
    @Inject(method = "removeTicketIf", at = @At("HEAD"), cancellable = true)
    private void felt$removeIf(TicketStorage.TicketPredicate predicate, it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<java.util.List<Ticket>> removed, CallbackInfo ci) {
        if (this.felt$manager() == null) return;
        for (var entry : this.felt$manager().getTicketsCopy().long2ObjectEntrySet()) for (Ticket ticket : entry.getValue()) {
            if (!predicate.test(ticket, entry.getLongKey())) continue;
            if (((TicketStorage)(Object)this).removeTicket(entry.getLongKey(), ticket) && removed != null) removed.computeIfAbsent(entry.getLongKey(), key -> new java.util.ArrayList<>()).add(ticket);
        }
        ci.cancel();
    }
}
