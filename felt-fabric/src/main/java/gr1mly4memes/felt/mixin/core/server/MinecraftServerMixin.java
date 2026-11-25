package gr1mly4memes.felt.mixin.core.server;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.mojang.datafixers.DataFixer;
import gr1mly4memes.felttools.api.FeltVersion;
import gr1mly4memes.felt.bridge.bukkit.CraftServerBridge;
import gr1mly4memes.felt.bridge.core.command.CommandSourceBridge;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.bridge.core.world.level.entity.PersistentEntitySectionManagerBridge;
import gr1mly4memes.felt.mod.FeltConstants;
import gr1mly4memes.felt.mod.mixins.annotation.TransformAccess;
import gr1mly4memes.felt.mod.server.FeltServer;
import gr1mly4memes.felt.mod.server.BukkitRegistry;
import gr1mly4memes.felt.mod.server.world.border.FeltBorderChangeListener;
import gr1mly4memes.felt.mod.server.world.border.FeltDelegatedBorderListener;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felt.util.IteratorUtil;
import gr1mly4memes.felt.mod.config.FeltConfig;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import io.papermc.paper.threadedregions.EntityScheduler;
import it.unimi.dsi.fastutil.longs.LongIterator;
import joptsimple.OptionSet;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.util.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.Services;
import net.minecraft.server.TickTask;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ChunkLoadCounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.TimeSource;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import gr1mly4memes.felt.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spigotmc.WatchdogThread;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.net.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerBridge, CommandSourceBridge {

    // @formatter:off
    @Shadow private int tickCount;
    @Shadow protected long nextTickTimeNanos;
    @Shadow @Final static Logger LOGGER;
    @Shadow public abstract Commands getCommands();
    @Shadow public abstract ServerLevel overworld();
    @Shadow private Map<ResourceKey<Level>, ServerLevel> levels;
    @Shadow protected abstract void setupDebugLevel(WorldData p_240778_1_);
    @Shadow protected WorldData worldData;
    @Shadow private static void setInitialSpawn(ServerLevel p_177897_, ServerLevelData p_177898_, boolean p_177899_, boolean p_177900_, LevelLoadListener listener) { }
    @Shadow @Final public Executor executor;
    @Shadow public abstract RegistryAccess.Frozen registryAccess();
    @Shadow public MinecraftServer.ReloadableResources resources;
    @Shadow public abstract LayeredRegistryAccess<RegistryLayer> registries();
    @Shadow public abstract Iterable<ServerLevel> getAllLevels();
    @Shadow public abstract void halt(boolean waitForServer);
    @Shadow @Final public LevelLoadListener levelLoadListener;
    @Shadow private void updateEffectiveRespawnData() { }
    // @formatter:on

    @Shadow private PlayerList playerList;

    @Override
    public void bridge$setRespawnWorld(ServerLevel level) {
        var root = (PrimaryLevelDataBridge) this.worldData.overworldData();
        root.bridge$setRespawnDimension(level.dimension());
        this.updateEffectiveRespawnData();
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "findRespawnDimension", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/storage/LevelData$RespawnData;dimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<Level> felt$useSelectedRespawnDimension(ResourceKey<Level> vanilla) {
        return ((PrimaryLevelDataBridge) this.worldData.overworldData()).bridge$getRespawnDimension();
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "updateEffectiveRespawnData", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;getWorldBorderAdjustedRespawnData(Lnet/minecraft/world/level/storage/LevelData$RespawnData;)Lnet/minecraft/world/level/storage/LevelData$RespawnData;"))
    private net.minecraft.world.level.storage.LevelData.RespawnData felt$selectWorldSpawn(ServerLevel level,
            net.minecraft.world.level.storage.LevelData.RespawnData vanilla,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.level.storage.LevelData.RespawnData> original) {
        var local = level.serverLevelData.getRespawnData();
        var respawn = net.minecraft.world.level.storage.LevelData.RespawnData.of(level.dimension(), local.pos(), local.yaw(), local.pitch());
        ((PrimaryLevelDataBridge) this.worldData.overworldData()).bridge$setVanillaRespawnData(respawn);
        return original.call(level, respawn);
    }

    public MinecraftServerMixin(String name, boolean propagatesCrashes) {
        super(name, propagatesCrashes);
    }

    public WorldLoader.DataLoadContext worldLoaderContext; // Paper added
    private boolean forceTicks;

    public CraftServer server;

    public OptionSet options;

    @Override
    public OptionSet bridge$getOptions() {
        return this.options;
    }

    public ConsoleCommandSender console;

    public RemoteConsoleCommandSender remoteConsole;

    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public int autosavePeriod;

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "forceGameTimeSynchronization", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void felt$worldGameTimeSync(net.minecraft.server.players.PlayerList list, net.minecraft.network.protocol.Packet<?> packet,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        for (net.minecraft.server.level.ServerPlayer player : list.getPlayers()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(player.level().getGameTime(), java.util.Map.of()));
        }
    }


    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;tick()V"))
    private void felt$tickWorldClocks(net.minecraft.world.clock.ServerClockManager global,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (io.papermc.paper.configuration.GlobalConfiguration.get().time.affectsAllWorlds) {
            original.call(global);
        } else {
            for (ServerLevel level : ((MinecraftServer) (Object) this).getAllLevels()) {
                original.call(level.clockManager());
            }
        }
    }


    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "onGameRuleChanged",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void felt$clockRuleSync(net.minecraft.server.players.PlayerList list, net.minecraft.network.protocol.Packet<?> packet,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundSetTimePacket) {
            var clocks = (gr1mly4memes.felt.bridge.core.world.clock.ServerClockOwnerBridge)
                ((MinecraftServer) (Object) this).clockManager();
            for (net.minecraft.server.level.ServerPlayer player : list.getPlayers()) {
                player.connection.send(clocks.createFullSyncPacket(player));
            }
        } else {
            original.call(list, packet);
        }
    }

    @Override
    public void bridge$setAutosavePeriod(int period) {
        this.autosavePeriod = period;
    }

    public Commands vanillaCommandDispatcher;
    private boolean hasStopped = false;
    @Unique private volatile boolean isRestarting; // Paper added
    @Shadow private int emptyTicks;
    @Shadow protected abstract int pauseWhenEmptySeconds();
    @Unique private final java.util.concurrent.ConcurrentMap<String, org.bukkit.plugin.Plugin> felt$pauseBlockers =
        new java.util.concurrent.ConcurrentHashMap<>();
    @Unique private int felt$currentTick;
    @Unique private long currentTickStart; // Paper added

    @Override
    public int bridge$getCurrentTick() {
        return this.felt$currentTick;
    }

    @Override
    public boolean bridge$isTickPaused() {
        return this.emptyTicks > 0 && this.emptyTicks >= this.pauseWhenEmptySeconds() * 20;
    }

    @Override
    public void bridge$allowPausing(org.bukkit.plugin.Plugin plugin, boolean allow) {
        if (allow) {
            this.felt$pauseBlockers.remove(plugin.getName());
        } else {
            this.felt$pauseBlockers.put(plugin.getName(), plugin);
        }
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "tickServer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/ServerTickRateManager;isSprinting()Z"))
    private boolean felt$preventsEmptyServerPause(boolean sprinting) {
        return sprinting || !this.felt$pauseBlockers.isEmpty();
    }

    @Decorate(method = "runServer()V", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isPaused()Z"), require = 1, allow = 1)
    private void felt$recordTickStart() {
        // Paper measures from the loop iteration, including scheduling and queued packets.
        this.currentTickStart = System.nanoTime();
    }

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;processPacketsAndTick(Z)V"))
    private void felt$countServerIteration(CallbackInfo ci) {
        ++this.felt$currentTick;
    }
    private final Object stopLock = new Object();

    private static final int TPS = 20;
    private static final int TICK_TIME = 1000000000 / TPS;
    private static final int SAMPLE_INTERVAL = 100;
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static int currentTick = (int) (System.currentTimeMillis() / 50);

    public final double[] recentTps = new double[3];

    @Override
    public double[] bridge$getRecentTps() {
        return this.recentTps.clone();
    }

    @Unique
    private long felt$tpsTickSection;
    @Unique
    private long felt$tpsTickCount;

    public boolean hasStopped() {
        synchronized (stopLock) {
            return hasStopped;
        }
    }

    @Override
    public boolean bridge$hasStopped() {
        return this.hasStopped();
    }

    @Override
    public void bridge$safeShutdown(boolean waitForServer, boolean restarting) {
        // Publish the restart intent before the native server loop begins shutdown.
        this.isRestarting = restarting;
        this.halt(waitForServer);
    }

    @Override
    public boolean bridge$isRestarting() {
        return this.isRestarting;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void felt$loadOptions(Thread p_236723_, LevelStorageSource.LevelStorageAccess p_236724_, PackRepository p_236725_, WorldStem worldStem, Optional<GameRules> gameRules, Proxy p_236727_, DataFixer p_236728_, Services p_236729_, LevelLoadListener p_236730_, boolean propagatesCrashes, NotificationManager notificationManager, CallbackInfo ci) {
        this.options = FeltCaptures.getBukkitOptions();
        this.vanillaCommandDispatcher = worldStem.dataPackResources().getCommands();
        this.worldLoaderContext = FeltCaptures.getDataLoadContext();
        FeltServer.setMinecraftServer((MinecraftServer) (Object) this);
    }

    @Decorate(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;"))
    private ServerStatus felt$initTickParam(MinecraftServer instance) throws Throwable {
        var serverStatus = (ServerStatus) DecorationOps.callsite().invoke(instance);
        Arrays.fill(recentTps, 20);
        this.felt$tpsTickSection = Util.getMillis();
        this.felt$tpsTickCount = 1;
        return serverStatus;
    }

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;", shift = At.Shift.AFTER))
    private void felt$enableSparkBeforePlugins(CallbackInfo ci) {
        this.server.spark.enableBeforePlugins();
    }

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;waitUntilNextTick()V"))
    private void felt$updateTickParam(CallbackInfo ci) {
        if (this.felt$tpsTickCount++ % SAMPLE_INTERVAL == 0) {
            long curTime = Util.getMillis();
            double currentTps = 1E3 / (curTime - this.felt$tpsTickSection) * SAMPLE_INTERVAL;
            recentTps[0] = calcTps(recentTps[0], 0.92, currentTps); // 1/exp(5sec/1min)
            recentTps[1] = calcTps(recentTps[1], 0.9835, currentTps); // 1/exp(5sec/5min)
            recentTps[2] = calcTps(recentTps[2], 0.9945, currentTps); // 1/exp(5sec/15min)
            this.felt$tpsTickSection = curTime;
        }
        currentTick = (int) (System.currentTimeMillis() / 50);
    }

    @Decorate(method = "runServer", at = @At(value = "INVOKE", remap = false, target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void felt$warnOnLoad(Logger instance, String s, Object o, Object o2) throws Throwable {
        if (server.getWarnOnOverload()) {
            DecorationOps.callsite().invoke(instance, s, o, o2);
        }
    }

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;onServerExit()V"))
    private void felt$watchdogExit(CallbackInfo ci) {
        WatchdogThread.doStop();
    }

    private static double calcTps(double avg, double exp, double tps) {
        return (avg * exp) + (tps * (1 - exp));
    }

    @Inject(method = "stopServer", cancellable = true, at = @At("HEAD"))
    public void felt$setStopped(CallbackInfo ci) {
        synchronized (stopLock) {
            if (hasStopped) {
                ci.cancel();
                return;
            }
            hasStopped = true;
        }
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", remap = false, ordinal = 0, shift = At.Shift.AFTER, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V"))
    public void felt$unloadPlugins(CallbackInfo ci) {
        if (this.server != null) {
            try {
                this.server.spark.disable();
                this.server.disablePlugins();
            } finally {
                this.server.shutdownPaperSchedulers();
            }
        }
    }

    @Decorate(method = "createLevels", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
    private Iterator<Map.Entry<ResourceKey<LevelStem>, LevelStem>> felt$skipBukkitLevels(Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> instance) throws Throwable {
        final var iterator = (Iterator<Map.Entry<ResourceKey<LevelStem>, LevelStem>>) DecorationOps.callsite().invoke(instance);
        if (FeltConfig.replaceableBaseWorldGenerator) {
            return IteratorUtil.filter(iterator, it -> {
                final var location = it.getKey().identifier();
                if (location.getNamespace().equals("bukkit")) {
                    FeltServer.LOGGER.info("Deferred {} custom dimension creation", location);
                    return false;
                } else {
                    return true;
                }
            });
        } else {
            return iterator;
        }
    }

    @Inject(method = "createLevels", at = @At("RETURN"))
    public void felt$enablePlugins(CallbackInfo ci) {
        this.bridge$mod$unlockRegistries();
        this.server.enablePlugins(PluginLoadOrder.POSTWORLD);
        this.server.spark.registerCommandBeforePlugins(this.server);
        this.server.spark.enableAfterPlugins(this.server);
        this.bridge$mod$lockRegistries();
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
    }

    private void executeModerately() {
        this.runAllTasks();
        this.bridge$drainQueuedTasks();
        java.util.concurrent.locks.LockSupport.parkNanos("executing tasks", 1000L);
    }

    @Override
    public void bridge$drainQueuedTasks() {
        Runnable task;
        while ((task = this.processQueue.poll()) != null) {
            task.run();
        }
    }

    @Inject(method = "haveTime", cancellable = true, at = @At("HEAD"))
    private void felt$forceAheadOfTime(CallbackInfoReturnable<Boolean> cir) {
        if (this.forceTicks) cir.setReturnValue(true);
    }

    @Inject(method = "createLevels", at = @At(value = "NEW", ordinal = 0, target = "(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/world/level/storage/ServerLevelData;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/dimension/LevelStem;ZJLjava/util/List;Z)Lnet/minecraft/server/level/ServerLevel;"))
    private void felt$registerEnv(CallbackInfo ci) {
        BukkitRegistry.registerEnvironments(this.registryAccess().lookupOrThrow(Registries.LEVEL_STEM));
    }

    @Decorate(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;addWorldborderListener(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void felt$configurableDelegatedListener(PlayerList instance, ServerLevel level) throws Throwable {
        // Felt: move world border listener initialization to world registration
        if (FeltDelegatedBorderListener.isEnabled()) {
            DecorationOps.callsite().invoke(instance, level);
        }
    }

    @Decorate(method = "createLevels", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object felt$worldInit(Map<Object, Object> instance, Object k, Object v) throws Throwable {
        if (v instanceof ServerLevel level) {
            if (((CraftServer) Bukkit.getServer()).scoreboardManager == null) {
                ((CraftServer) Bukkit.getServer()).scoreboardManager = new CraftScoreboardManager((MinecraftServer) (Object) this, level.getScoreboard());
            }
            if (((WorldBridge) level).bridge$getGenerator() != null) {
                level.bridge$getWorld().getPopulators().addAll(
                    ((WorldBridge) level).bridge$getGenerator().getDefaultPopulators(
                        level.bridge$getWorld()));
            }
            Bukkit.getPluginManager().callEvent(new WorldInitEvent(level.bridge$getWorld()));

            // Felt: move world border listener initialization to world registration
            // Felt: FeltBorderChangeListener is singleton so won't be added more than once
            // Felt: multiple Decorators cannot target the same method here.
            level.getWorldBorder().addListener(FeltBorderChangeListener.typed());
        }
        return DecorationOps.callsite().invoke(instance, k, v);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public final void prepareLevels() {
        for (ServerLevel serverWorld : this.levels.values()) {
            this.prepareLevels(serverWorld);
            this.felt$tickPendingEntities(serverWorld);
            Bukkit.getPluginManager().callEvent(new WorldLoadEvent(serverWorld.bridge$getWorld()));
        }
        this.updateEffectiveRespawnData();
    }

    @Unique
    private void felt$tickPendingEntities(ServerLevel level) {
        if ((Object) level instanceof ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel world
            && world.moonrise$getChunkTaskScheduler() != null) {
            world.moonrise$getChunkTaskScheduler().executeAllRecentlyQueuedMainThreadTasks();
            return;
        }
        level.entityManager.tick(); // Make pending entities available to WorldLoadEvent listeners.
    }

    @Unique
    private void felt$setupWorldDebugLevel(WorldData data, ServerLevel level) {
        // Match CraftBukkit's world-local debug setup without replacing the native helper.
        data.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL);
        data.setDifficultyLocked(true);
        ((gr1mly4memes.felt.bridge.core.world.level.GameRulesBridge) level.getGameRules())
            .bridge$setWorldRule(GameRules.ADVANCE_WEATHER, false, level);
        level.clockManager().moveToTimeMarker(
            this.registryAccess().getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD),
            net.minecraft.world.clock.ClockTimeMarkers.NOON);
        data.overworldData().setGameType(net.minecraft.world.level.GameType.SPECTATOR);
    }

    // bukkit methods
    public void initWorld(ServerLevel serverWorld, ServerLevelData worldInfo, WorldData saveData, WorldOptions worldOptions) {
        boolean flag = saveData.isDebugWorld();
        if (((WorldBridge) serverWorld).bridge$getGenerator() != null) {
            serverWorld.bridge$getWorld().getPopulators().addAll(
                ((WorldBridge) serverWorld).bridge$getGenerator().getDefaultPopulators(
                    serverWorld.bridge$getWorld()));
        }
        WorldBorder worldborder = serverWorld.getWorldBorder();
        worldborder.applyInitialSettings(0L);

        // Felt: move world border listener initialization to world registration
        playerList.addWorldborderListener(serverWorld);

        // Call WorldInitEvent for Bukkit created world
        // Before any chunk is loaded/generated.
        // This makes delayed configurate possible.
        // Calling multiple times is OK since Spigot also do so.
        // See [PlotSquared] BukkitSetupUtils#setupWorld(PlotAreaBuilder).
        // See CraftServer.
        // CraftBukkit - SPIGOT-5569: Call WorldInitEvent before any chunks are generated
        this.server.getPluginManager().callEvent(new WorldInitEvent(serverWorld.bridge$getWorld()));

        if (!worldInfo.isInitialized()) {
            try {
                setInitialSpawn(serverWorld, worldInfo, worldOptions.generateBonusChest(), flag, this.levelLoadListener);
                worldInfo.setInitialized(true);
                if (flag) {
                    this.felt$setupWorldDebugLevel(saveData, serverWorld);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception initializing level");
                try {
                    serverWorld.fillReportDetails(crashreport);
                } catch (Throwable throwable2) {
                    // empty catch block
                }
                throw new ReportedException(crashreport);
            }
            worldInfo.setInitialized(true);
        }
    }

    // bukkit methods
    public void prepareLevels(ServerLevel serverWorld) {
        this.bridge$mod$markLevelsDirty();
        this.forceTicks = true;
        ChunkLoadCounter chunkLoadCounter = new ChunkLoadCounter();
        chunkLoadCounter.track(serverWorld, () -> {
            TicketStorage forcedchunkssavedata = serverWorld.getDataStorage().get(TicketStorage.TYPE);
            if (forcedchunkssavedata != null) {
                forcedchunkssavedata.activateAllDeactivatedTickets();
                LongIterator longiterator = forcedchunkssavedata.getForceLoadedChunks().iterator();

                while (longiterator.hasNext()) {
                    long k = longiterator.nextLong();
                    ChunkPos chunkpos = ChunkPos.unpack(k);
                    serverWorld.getChunkSource().updateChunkForced(chunkpos, true);
                }
                this.bridge$mod$reinstatePersistentChunks(serverWorld, forcedchunkssavedata);
            }
        });

        this.levelLoadListener.start(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, chunkLoadCounter.totalChunks());

        do {
            this.levelLoadListener.update(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, chunkLoadCounter.readyChunks(), chunkLoadCounter.totalChunks());
            this.executeModerately();
        } while (chunkLoadCounter.pendingChunks() > 0);

        this.levelLoadListener.finish(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS);
        this.forceTicks = false;
    }

    // bukkit callbacks
    public void addLevel(ServerLevel level) {
        this.levels.put(level.dimension(), level);
        this.felt$onServerLoad(level);
        this.bridge$mod$markLevelsDirty();
    }

    public void removeLevel(ServerLevel level) {
        if (!this.levels.remove(level.dimension(), level)) {
            return;
        }
        Throwable failure = null;
        try {
            this.felt$onServerUnload(level);
        } catch (RuntimeException | Error ex) {
            failure = ex;
        }
        try {
            // Include queued schedulers and idle wrappers before callbacks can mutate either source.
            Set<EntityScheduler> schedulers = Collections.newSetFromMap(new IdentityHashMap<>());
            Collections.addAll(schedulers, this.server.getEntitySchedulerTickList().getAllSchedulers());
            for (var entity : felt$loadedEntities(level)) {
                var wrapper = ((EntityBridge) entity).bridge$peekBukkitEntity();
                if (wrapper != null) {
                    schedulers.add(wrapper.taskScheduler);
                }
            }
            for (var scheduler : schedulers) {
                try {
                    // A previous retired callback may have transferred this wrapper to another world.
                    if (scheduler.entity.getHandleRaw().level() == level && !scheduler.isRetired()) {
                        scheduler.retire();
                    }
                } catch (RuntimeException | Error ex) {
                    if (failure == null) failure = ex;
                    else if (failure != ex) failure.addSuppressed(ex);
                }
            }
        } catch (RuntimeException | Error ex) {
            if (failure == null) failure = ex;
            else if (failure != ex) failure.addSuppressed(ex);
        }
        try {
            this.bridge$mod$markLevelsDirty();
        } catch (RuntimeException | Error ex) {
            if (failure == null) failure = ex;
            else if (failure != ex) failure.addSuppressed(ex);
        }
        try {
            ((CraftServerBridge) Bukkit.getServer()).bridge$removeWorld(level);
        } catch (RuntimeException | Error ex) {
            if (failure == null) failure = ex;
            else if (failure != ex) failure.addSuppressed(ex);
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure != null) {
            throw (RuntimeException) failure;
        }
    }

    @Override
    public void bridge$removeLevel(ServerLevel level) {
        this.removeLevel(level);
    }

    public boolean isIteratingOverLevels;

    @Inject(method = "tickChildren(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;updateEffectiveRespawnData()V", shift = At.Shift.AFTER), require = 1)
    private void felt$beginWorldIteration(BooleanSupplier haveTime, CallbackInfo ci) {
        this.isIteratingOverLevels = true;
    }

    @Inject(method = "tickChildren(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=connection"), require = 1)
    private void felt$endWorldIteration(BooleanSupplier haveTime, CallbackInfo ci) {
        this.isIteratingOverLevels = false;
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    public void felt$runScheduler(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        FeltConstants.currentTick = (int) (System.currentTimeMillis() / 50);
        this.server.getScheduler().mainThreadHeartbeat();
        this.server.tickPaperSchedulers();
        this.bridge$drainQueuedTasks();
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZ)Z"))
    private void felt$unloadLevel(CallbackInfo ci) {
        for (var serverLevel : this.getAllLevels()) {
            ((CraftServerBridge) Bukkit.getServer()).bridge$removeWorld(serverLevel);
        }
    }

    // Every ServerLevel saves its own level.dat, respecting skipSave. Keep the global
    // SavedData flush below this call: mods may still store unrelated global data there.
    @com.llamalad7.mixinextras.injector.WrapWithCondition(method = "saveAllChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;saveDataTag(Lnet/minecraft/world/level/storage/WorldData;Ljava/util/UUID;)V"))
    private boolean felt$worldOwnsLevelDat(LevelStorageSource.LevelStorageAccess access, WorldData data, java.util.UUID playerId) {
        return false;
    }

    @Inject(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/WorldData;setDataConfiguration(Lnet/minecraft/world/level/WorldDataConfiguration;)V"))
    private void felt$syncCommand(CallbackInfo ci) {
        this.server.syncCommands();
    }

    @Inject(method = "getServerModName", remap = false, cancellable = true, at = @At("RETURN"))
    private void felt$brand(CallbackInfoReturnable<String> cir) {
        var version = FeltVersion.current();
        cir.setReturnValue(version.getReleaseName() == null ? "Felt" : "Felt " + version.getReleaseName());
    }

    private boolean felt$skipWatchdogSetTime = false;

    @Override
    public void felt$extendNextTickTimeTo(TimeSource.NanoTimeSource timeSource) {
        if (!felt$skipWatchdogSetTime) {
            this.nextTickTimeNanos = timeSource.getAsLong();
        }
    }

    protected void felt$tickSpigotWatchdogInternal() {
        try {
            felt$skipWatchdogSetTime = true;
            WatchdogThread.tick();
        } finally {
            felt$skipWatchdogSetTime = false;
        }
    }

    @Decorate(method = "tickServer(Ljava/util/function/BooleanSupplier;)V", inject = true,
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;tickCount:I", opcode = Opcodes.GETFIELD, ordinal = 0), require = 1, allow = 1)
    private void felt$serverTickStart() {
        // This edge is after the empty-server return, immediately before the native increment.
        this.server.spark.tickStart();
        new ServerTickStartEvent(this.tickCount + 1).callEvent();
    }

    @Decorate(method = "tickServer(Ljava/util/function/BooleanSupplier;)V", inject = true,
        at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=tallying"), require = 1, allow = 1)
    private void felt$serverTickEnd() {
        long endTime = System.nanoTime();
        // Native deadlines use Util's clock, which mods may replace independently of nanoTime.
        long remaining = this.nextTickTimeNanos - Util.getNanos();
        this.server.spark.executeMainThreadTasks();
        new ServerTickEndEvent(this.tickCount, (double) (endTime - this.currentTickStart) / 1_000_000D, remaining).callEvent();
        this.server.spark.tickEnd((double) (endTime - this.currentTickStart) / 1_000_000D);
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickConnection()V", ordinal = 0, shift = At.Shift.BEFORE))
    private void felt$emptyTickSparkStart(CallbackInfo ci) {
        // Empty-server paused branch: Paper starts spark before draining queued tasks.
        this.server.spark.tickStart();
        this.server.getScheduler().mainThreadHeartbeat();
        this.bridge$drainQueuedTasks();
        for (ServerLevel level : this.getAllLevels()) {
            level.getChunkSource().tick(() -> true, false);
        }
        this.server.spark.executeMainThreadTasks();
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickConnection()V", ordinal = 0, shift = At.Shift.AFTER))
    private void felt$emptyTickSparkEnd(CallbackInfo ci) {
        this.server.spark.tickEnd((double) (System.nanoTime() - this.currentTickStart) / 1_000_000D);
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void felt$tickWatchdog(CallbackInfo ci) {
        this.felt$pauseBlockers.values().removeIf(plugin -> !plugin.isEnabled());
        felt$tickSpigotWatchdogInternal();
    }

    @Override
    public ConsoleCommandSender bridge$getConsole() {
        return this.console;
    }

    @Override
    public void bridge$setConsole(ConsoleCommandSender console) {
        this.console = console;
    }

    @Override
    public void bridge$setServer(CraftServer server) {
        this.server = server;
    }

    // Used for one-shot cache access
    @Override
    public CraftServer bridge$getServer() {
        if (this.server == null) {
            throw new IllegalStateException("CraftServer has not been initialized yet");
        }
        return this.server;
    }

    @Override
    public RemoteConsoleCommandSender bridge$getRemoteConsole() {
        return remoteConsole;
    }

    @Override
    public void bridge$queuedProcess(Runnable runnable) {
        processQueue.add(runnable);
    }

    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return console;
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender(wrapper);
    }

    @Override
    public Commands bridge$getVanillaCommands() {
        return this.vanillaCommandDispatcher;
    }

    public boolean isDebugging() {
        return false;
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static MinecraftServer getServer() {
        return FeltServer.getMinecraftServer();
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    @Deprecated
    private static RegistryAccess getDefaultRegistryAccess() {
        return CraftRegistry.getMinecraftRegistry();
    }

    public double[] getTPS() {
        return this.bridge$getRecentTps();
    }

    public net.kyori.adventure.text.Component motd() {
        return this.bridge$getMotd();
    }

    public void motd(net.kyori.adventure.text.Component motd) {
        this.bridge$setMotd(motd);
    }

    public boolean isTickPaused() {
        return this.bridge$isTickPaused();
    }

    public void safeShutdown(boolean waitForServer, boolean restarting) {
        this.bridge$safeShutdown(waitForServer, restarting);
    }


    @Unique
    private static Iterable<net.minecraft.world.entity.Entity> felt$loadedEntities(ServerLevel level) {
        if ((Object) level instanceof ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemLevel world
            && world.moonrise$getEntityLookup() != null) return world.moonrise$getEntityLookup().getAll();
        return ((PersistentEntitySectionManagerBridge) level.entityManager).bridge$getExistingEntities();
    }
}

