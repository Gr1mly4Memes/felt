package gr1mly4memes.felt.mixin.core.world.clock;

import gr1mly4memes.felt.bridge.core.world.clock.ServerClockOwnerBridge;
import gr1mly4memes.felt.bridge.core.world.clock.ClockInstanceBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.bukkit.Bukkit;
import org.bukkit.event.world.TimeSkipEvent;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.PackedClockStates;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** World/global clock ownership with player-specific synchronization and scoped sleep events. */
@Mixin(ServerClockManager.class)
public abstract class ServerClockManagerMixin implements ServerClockOwnerBridge {
    @Shadow public abstract void init(MinecraftServer server);
    @Shadow public abstract PackedClockStates packState();
    @Shadow public abstract void setTotalTicks(Holder<WorldClock> clock, long totalTicks);

    @Shadow private MinecraftServer server;

    @Override
    public void bridge$addTicks(Holder<WorldClock> clock, long ticks) {
        ((ServerClockManagerAccessor) this).felt$modifyClock(clock, instance -> {
            ClockInstanceBridge state = (ClockInstanceBridge) instance;
            state.bridge$totalTicks(Math.max(state.bridge$totalTicks() + ticks, 0L));
        });
    }

    @Override
    public java.util.OptionalLong bridge$timeMarker(Holder<WorldClock> clock,
            net.minecraft.resources.ResourceKey<net.minecraft.world.clock.ClockTimeMarker> marker) {
        ClockInstanceBridge instance = (ClockInstanceBridge) java.util.Objects.requireNonNull(((ServerClockManagerAccessor) this).felt$clocks().get(clock), "No clock initialized for definition: " + clock);
        var value = instance.bridge$timeMarkers().get(marker);
        return value == null ? java.util.OptionalLong.empty()
            : java.util.OptionalLong.of(value.resolveTimeToMoveTo(instance.bridge$totalTicks()));
    }

    /** Run the supplied native/mod mutation once. Veto exits before partialTick and update tail. */
    @WrapMethod(method = "modifyClock(Lnet/minecraft/core/Holder;Ljava/util/function/Consumer;)V", require = 1, expect = 1, allow = 1)
    private void felt$nightMutation(Holder<WorldClock> clock, Consumer<?> action, Operation<Void> original) {
        var manager = ((ServerClockManager) (Object) this);
        ServerLevel owner = (ServerLevel) FeltCaptures.nightLevel;
        boolean sleep = owner != null && owner.dimensionType().defaultClock().map(clock::is).orElse(false);
        FeltCaptures.nightProposal = sleep && FeltCaptures.nightManager == manager && FeltCaptures.nightClock == clock ? (before, proposed) -> {
            TimeSkipEvent event = new TimeSkipEvent(((WorldBridge) owner).bridge$getWorld(),
                io.papermc.paper.world.PaperWorldClock.minecraftHolderToBukkit(clock), TimeSkipEvent.SkipReason.NIGHT_SKIP, proposed - before);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                FeltCaptures.nightCancelled = true;
                throw new FeltCaptures.NightSkipCancelled();
            }
            return before + event.getSkipAmount();
        } : null;
        try {
            try {
                original.call(clock, action);
            } catch (FeltCaptures.NightSkipCancelled ignored) {
                // The sleep event rejected this clock mutation.
            }
        } finally {
            FeltCaptures.nightProposal = null;
        }
    }

    // The original marker resolver/add clamp computes the value. No synthetic algorithm or replay.
    // Strict three-site requirement: one totalTicks PUTFIELD in each actual mutation lambda.
    @WrapOperation(method = {
        "lambda$moveToTimeMarker$0(Lnet/minecraft/resources/ResourceKey;Lorg/apache/commons/lang3/mutable/MutableObject;Lnet/minecraft/world/clock/ServerClockManager$ServerClockInstance;)V",
        "lambda$setTotalTicks$0(JLnet/minecraft/world/clock/ServerClockManager$ServerClockInstance;)V",
        "lambda$addTicks$0(ILnet/minecraft/world/clock/ServerClockManager$ServerClockInstance;)V"
    }, at = @At(value = "FIELD", target = "Lnet/minecraft/world/clock/ServerClockManager$ServerClockInstance;totalTicks:J", opcode = Opcodes.PUTFIELD), require = 3, expect = 3, allow = 3)
    private static void felt$nightWrite(@Coerce Object instance, long proposed, Operation<Void> original) {
        long ticks = FeltCaptures.nightProposal == null ? proposed
            : FeltCaptures.nightProposal.applyAsLong(((ClockInstanceBridge) instance).bridge$totalTicks(), proposed);
        original.call(instance, ticks);
    }

    @Unique private ServerLevel level; // Paper added

    @Override
    public void init(ServerLevel level) {
        if (this.level != null) {
            if (this.level != level) {
                throw new IllegalStateException("Clock manager cannot belong to two worlds");
            }
            return; // Native init recreates all instances: never overwrite previously loaded values.
        }
        this.init(level.getServer()); // Native registry/markers + packed state load.
        this.level = level;
    }

    @Override
    public ServerLevel felt$owner() {
        return this.level;
    }

    @Inject(method = "getGameTime", at = @At("HEAD"), cancellable = true)
    private void felt$worldGameTime(CallbackInfoReturnable<Long> cir) {
        if (this.level != null) {
            cir.setReturnValue(this.level.getGameTime());
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGlobalGameRules()Lnet/minecraft/world/level/gamerules/GameRules;"))
    private GameRules felt$worldAdvanceRule(MinecraftServer server) {
        return this.level == null ? server.getGlobalGameRules() : this.level.getGameRules();
    }

    // Native modifyClock still performs mutation, dirty marking and cache invalidation exactly once.
    // Send player-specific packets to the selected owner scope.
    @Redirect(method = "modifyClock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void felt$worldBroadcast(PlayerList list, Packet<?> packet) {
        if (!(packet instanceof ClientboundSetTimePacket time)) {
            list.broadcastAll(packet);
            return;
        }
        for (ServerPlayer player : this.level == null ? list.getPlayers() : this.level.players()) {
            Map<Holder<WorldClock>, ClockNetworkState> updates = new HashMap<>();
            for (Holder<WorldClock> clock : time.clockUpdates().keySet()) {
                ClockState state = this.packState().clocks().get(clock);
                if (state != null) {
                    updates.put(clock, this.felt$networkState(clock, state, player));
                }
            }
            player.connection.send(new ClientboundSetTimePacket(this.felt$gameTime(), updates));
        }
    }

    @Redirect(method = "modifyClock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getAllLevels()Ljava/lang/Iterable;"))
    private Iterable<ServerLevel> felt$worldCacheScope(MinecraftServer server) {
        return this.level == null ? server.getAllLevels() : java.util.List.of(this.level);
    }

    @Override
    public ClientboundSetTimePacket createFullSyncPacket(ServerPlayer player) {
        if (this.level != null && player.level() != this.level) {
            throw new IllegalArgumentException("Clock sync player belongs to a different world");
        }
        Map<Holder<WorldClock>, ClockNetworkState> clockUpdates = new HashMap<>();
        this.packState().clocks().forEach((clock, state) ->
            clockUpdates.put(clock, this.felt$networkState(clock, state, player)));
        return new ClientboundSetTimePacket(this.felt$gameTime(), clockUpdates);
    }

    @Unique
    private ClockNetworkState felt$networkState(Holder<WorldClock> clock, ClockState state, ServerPlayer player) {
        boolean advance = this.felt$worldAdvanceRule(this.server).get(GameRules.ADVANCE_TIME);
        boolean ticking = !state.paused() && advance;
        boolean defaultClock = player.level().dimensionType().defaultClock()
            .map(clock::is).orElse(false);
        long ticks = defaultClock ? felt$playerTime(player, state.totalTicks()) : state.totalTicks();
        return new ClockNetworkState(ticks, state.partialTick(), ticking && (!defaultClock || player.relativeTime) ? state.rate() : 0.0F);
    }

    @Unique
    private long felt$gameTime() {
        return this.level == null ? this.server.overworld().getGameTime() : this.level.getGameTime();
    }

    @Unique
    private static long felt$playerTime(ServerPlayer player, long ticks) {
        return ((gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge) player).bridge$getPlayerTime(ticks);
    }
}
