package gr1mly4memes.felt;

import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.common.util.TickThread;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType;
import com.mojang.authlib.services.ServicesKeySet;
import com.mojang.serialization.Lifecycle;
import gr1mly4memes.felt.mod.util.BukkitOptionParser;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.jsonrpc.JsonRpc;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelDataAndDimensions.WorldDataAndGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;

/** Starts a local dedicated server directly, using the vanilla GameTest world-loading recipe. */
public final class MoonriseWorldTest {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        FeltCaptures.captureBukkitOptions(new BukkitOptionParser().parse("--nogui"));
        io.papermc.paper.plugin.PluginInitializerManager.load(FeltCaptures.getBukkitOptions());
        Bootstrap.bootStrap();
        for (var entry : net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("main", net.fabricmc.api.ModInitializer.class)) entry.onInitialize();
        for (var entry : net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("server", net.fabricmc.api.DedicatedServerModInitializer.class)) entry.onInitializeServer();
        var freeze = net.minecraft.core.registries.BuiltInRegistries.class.getDeclaredMethod("freeze");
        freeze.setAccessible(true);
        freeze.invoke(null);
        Path root = Files.createTempDirectory(Path.of(".").toAbsolutePath(), "native-world-");
        Path properties = root.resolve("server.properties");
        Files.writeString(properties, "online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nview-distance=2\nsimulation-distance=2\nmax-tick-time=-1\nspawn-protection=0\nenable-status=false\n");
        var settings = new DedicatedServerSettings(properties);
        var storage = LevelStorageSource.createDefault(root).validateAndCreateAccess("world");
        var packs = ServerPacksSource.createPackRepository(storage);
        packs.reload();
        var flags = FeatureFlags.DEFAULT_FLAGS;
        var dataConfig = new WorldDataConfiguration(new DataPackConfig(List.of("vanilla"), List.of()), flags);
        var levelSettings = new LevelSettings("Native Moonrise verification", GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, dataConfig);
        var config = new WorldLoader.InitConfig(new WorldLoader.PackConfig(packs, dataConfig, false, true), Commands.CommandSelection.DEDICATED, LevelBasedPermissionSet.OWNER);
        var registries = PaperRegistryLifecycleTest.register();
        WorldStem stem = Util.blockUntilDone(executor -> WorldLoader.load(config, context -> {
            FeltCaptures.captureDataLoadContext(context);
            var presets = context.datapackWorldRegistries().lookupOrThrow(Registries.WORLD_PRESET);
            var dimensions = presets.getOrThrow(Boolean.getBoolean("felt.verify.noise") ? WorldPresets.NORMAL : WorldPresets.FLAT_ALL_DIMENSIONS).value().createWorldDimensions();
            var baked = dimensions.bake(new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze());
            var data = new PrimaryLevelData(levelSettings, baked.specialWorldProperty(), baked.lifecycle());
            return new WorldLoader.DataLoadOutput<>(new WorldDataAndGenSettings(data, new WorldGenSettings(new WorldOptions(123L, false, false), dimensions)), baked.dimensionsRegistryAccess());
        }, WorldStem::new, Util.backgroundExecutor(), executor)).get(60, TimeUnit.SECONDS);
        var notifications = new NotificationManager();
        var services = new Services(null, ServicesKeySet.EMPTY, null, new net.minecraft.server.players.UserNameToIdResolver() {
            public void add(net.minecraft.server.players.NameAndId profile) {}
            public Optional<net.minecraft.server.players.NameAndId> get(String name) { return Optional.empty(); }
            public Optional<net.minecraft.server.players.NameAndId> get(java.util.UUID id) { return Optional.empty(); }
            public void resolveOfflineUsers(boolean value) {}
            public void save() {}
        }, new net.minecraft.server.players.ProfileResolver() {
            public Optional<com.mojang.authlib.GameProfile> fetchByName(String name) { return Optional.empty(); }
            public Optional<com.mojang.authlib.GameProfile> fetchById(java.util.UUID id) { return Optional.empty(); }
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> thread = new AtomicReference<>();
        CompletableFuture<Void> finished = new CompletableFuture<>();
        MinecraftServer.spin(serverThread -> {
            thread.set(serverThread);
            var server = new DedicatedServer(serverThread, storage, packs, stem, Optional.empty(), settings, DataFixers.getDataFixer(), services, JsonRpc.create(settings, notifications), notifications) {
                private int verificationTicks;
                private boolean verifyEmptyPause;
                private int initEvents;
                private AutoCloseable initListener;
                private PaperCustomClickTest customClicks;
                @Override public int pauseWhenEmptySeconds() {
                    return this.verifyEmptyPause ? 1 : 0;
                }
                @Override protected boolean initServer() throws java.io.IOException {
                    try {
                        check(super.initServer(), "Dedicated server did not initialize");
                        var type = io.papermc.paper.threadedregions.RegionizedServerInitEvent.class;
                        this.initListener = PaperEventHooksTest.listen(io.papermc.paper.threadedregions.RegionizedServerInitEvent.getHandlerList(),
                            type, verificationPlugin(), event -> ++this.initEvents);
                        this.customClicks = new PaperCustomClickTest();
                    } catch (Throwable thrown) { failure.set(thrown); }
                    if (failure.get() != null) this.halt(false);
                    return true;
                }
                @Override protected void tickServer(java.util.function.BooleanSupplier haveTime) {
                    try {
                        super.tickServer(haveTime);
                        if (++this.verificationTicks == 5) {
                            this.initListener.close();
                            check(this.initEvents == 1, "Paper startup event did not fire exactly once before ticking");
                            this.customClicks.verify(this.overworld(), verificationPlugin());
                            verifyWorld(this.overworld());
                            for (ServerLevel dimension : this.getAllLevels()) if (dimension != this.overworld()) verifyDimension(dimension);
                            PaperGameplayApiTest.verifyRespawnWorld(this);
                            PaperWorldCreatorTest.verify();
                            registries.verify(this);
                            PaperRecipeBookTest.verify(this.overworld());
                            PaperProxyConnectionTest.verify(this);
                            PaperReconfigurationTest.verifyTracking(this.overworld());
                            PaperReconfigurationTest.verifyResourcePackStages(this.overworld(), verificationPlugin());
                            PaperReconfigurationTest.verify(this.overworld(), verificationPlugin());
                            this.verifyPausing(haveTime);
                            this.halt(false);
                        }
                    } catch (Throwable thrown) { failure.set(thrown); this.halt(false); }
                }
                private void verifyPausing(java.util.function.BooleanSupplier haveTime) {
                    this.verifyEmptyPause = true;
                    var craft = org.bukkit.Bukkit.getServer();
                    var enabled = new java.util.concurrent.atomic.AtomicBoolean(true);
                    var blocking = verificationPlugin("PauseFixture", enabled::get);
                    craft.allowPausing(blocking, false);
                    for (int index = 0; index < 21; index++) {
                        super.tickServer(haveTime);
                    }
                    check(!craft.isPaused(), "An enabled plugin could not veto empty-server pausing");
                    enabled.set(false);
                    for (int index = 0; index < 20; index++) {
                        super.tickServer(haveTime);
                    }
                    check(craft.isPaused(), "A disabled plugin kept blocking empty-server pausing");
                    var called = new java.util.concurrent.atomic.AtomicInteger();
                    var schedulerPlugin = verificationPlugin();
                    craft.getScheduler().runTask(schedulerPlugin, called::incrementAndGet);
                    ((gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge) this).bridge$queuedProcess(called::incrementAndGet);
                    int tick = this.getTickCount();
                    super.tickServer(haveTime);
                    check(craft.isPaused() && this.getTickCount() == tick && called.get() == 2,
                        "Paused server did not run Bukkit and queued work while leaving simulation paused");
                    enabled.set(true);
                    craft.allowPausing(blocking, false);
                    super.tickServer(haveTime);
                    check(!craft.isPaused() && this.getTickCount() == tick + 1, "A plugin veto did not wake the paused server");
                    craft.allowPausing(blocking, true);
                    System.out.println("Paper pause API: plugin veto, disabled-plugin cleanup and paused scheduler work passed");
                }
                @Override protected void onServerCrash(net.minecraft.CrashReport report) { failure.compareAndSet(null, new AssertionError(report.getFriendlyReport(net.minecraft.ReportType.CRASH))); }
                @Override protected void onServerExit() { finished.complete(null); }
            };
            server.setPort(0);
            notifications.setServer(server);
            return server;
        });
        finished.get(180, TimeUnit.SECONDS);
        thread.get().join(10_000L);
        if (failure.get() != null) throw new AssertionError("Native world verification failed", failure.get());
        System.out.println("Native Moonrise world: generation, promotion, lighting, entities, save, unload and reload passed");
    }

    private static void verifyWorld(ServerLevel world) throws Exception {
        check(TickThread.isTickThread(), "Server did not use a tick thread");
        var nativeWorld = (ChunkSystemServerLevel) world;
        var scheduler = nativeWorld.moonrise$getChunkTaskScheduler();
        var holders = scheduler.chunkHolderManager;
        ChunkPos pos = new ChunkPos(40, -40);
        var ticket = ChunkSystemTicketType.create("felt:world_verification", null);
        holders.addTicketAtLevel(ticket, pos, holders.ENTITY_TICKING_TICKET_LEVEL, null);
        CompletableFuture<ChunkAccess> loaded = new CompletableFuture<>();
        scheduler.scheduleChunkLoad(pos.x(), pos.z(), ChunkStatus.FULL, true, Priority.BLOCKING, loaded::complete);
        world.getChunkSource().mainThreadProcessor.managedBlock(loaded::isDone);
        check(loaded.join() instanceof LevelChunk, "Generation did not produce a full chunk");
        world.getChunkSource().mainThreadProcessor.managedBlock(() -> {
            holders.processTicketUpdates();
            return holders.getChunkHolder(pos.pack()).isEntityTickingReady();
        });
        BlockPos marker = new BlockPos((pos.x() << 4) + 8, 5, (pos.z() << 4) + 8);
        check(world.isPositionTickingWithEntitiesLoaded(pos.pack()), "Scheduled block ticks still use the legacy entity manager");
        check(world.isPositionEntityTicking(marker), "Native entity-ticking readiness was not exposed to the world");
        world.setBlockAndUpdate(marker, Blocks.GOLD_BLOCK.defaultBlockState());
        world.setBlockAndUpdate(marker.east(), Blocks.COMPOSTER.defaultBlockState());
        long poiDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        world.getServer().managedBlock(() -> {
            check(System.nanoTime() < poiDeadline, "POI update timed out");
            return world.getPoiManager().getType(marker.east()).isPresent();
        });
        check(world.getPoiManager().getType(marker.east()).isPresent(), "POI lookup missed a placed workstation");
        var pig = net.minecraft.world.entity.EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND);
        check(pig != null, "Could not create a test entity");
        pig.setPos(marker.getX() + 0.5, 6, marker.getZ() + 0.5);
        ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$pushAddEntityReason(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL);
        check(world.addFreshEntity(pig), "Could not add test entity");
        check(pig.getBukkitEntity().getEntitySpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL, "Initial spawn reason API did not record the original spawn");
        ((gr1mly4memes.felt.bridge.core.entity.InitialSpawnReasonBridge)pig).bridge$setInitialSpawnReason(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
        check(pig.getBukkitEntity().getEntitySpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL, "Later insertion overwrote the original spawn reason");
        check(pig.getBukkitEntity().getOrigin() != null && pig.getBukkitEntity().getOrigin().getY() == 6, "Entity origin did not capture the first tracked position");
        check(nativeWorld.moonrise$getEntityLookup().get(pig.getUUID()) == pig, "Entity lookup missed added entity");
        verifyEntityStorageLimits(world, pos, pig);
        check(java.util.Arrays.stream(io.papermc.paper.FeatureHooks.getChunkEntities(world, pos.x(), pos.z())).anyMatch(entity -> entity.getUniqueId().equals(pig.getUUID())), "Bukkit chunk entity lookup missed a native entity");
        var bukkitChunk = new org.bukkit.craftbukkit.CraftChunk((LevelChunk)loaded.join());
        check(bukkitChunk.isEntitiesLoaded() && java.util.Arrays.stream(bukkitChunk.getEntities()).anyMatch(entity -> entity.getUniqueId().equals(pig.getUUID())), "CraftChunk still queried the legacy entity manager");
        pig.setPos(pig.getX() + 1.0, 18.0, pig.getZ());
        check(world.getEntitiesOfClass(pig.getClass(), pig.getBoundingBox().inflate(0.1)).contains(pig), "Entity section movement did not update spatial lookup");
        verifySpawnAccounting(world, marker);
        var daylightMobs = verifyEntityControls(world, pig, marker);
        var friction = verifyFrictionControls(world, pig, marker);
        verifySpawningInfluence(world, marker);
        var armorStand = verifyArmorStandControls(world, marker);
        var fallingBlock = verifyFallingBlockControls(world, marker);
        var cubeMobs = verifyCubeMobControls(world, marker);
        verifyWitchDrinking(world, marker);
        var merchant = verifyMerchantDiscounts(world, marker);
        var attributed = PaperGameplayApiTest.verify(world, marker, pig, verificationPlugin());
        PaperGameplayApiTest.verifyDecorationDeathVibrations(world, marker.above(10), verificationPlugin());
        PaperEventHooksTest.verify(world, marker.atY(230), verificationPlugin());
        PaperDispenserApiTest.verify(world, marker.atY(224), verificationPlugin());
        verifyAnvilEnchantmentLimit(world, marker);
        verifyBlockControls(world, marker);
        verifyRedstoneImplementations(world, marker.offset(-5, 105, -5));
        verifyGrassSpreadAndFade(world, marker.atY(250));
        var lootFixture = verifyLootReplenishment(world, marker);
        world.setBlockAndUpdate(marker.above(), Blocks.TORCH.defaultBlockState());
        long lightDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        world.getChunkSource().mainThreadProcessor.managedBlock(() -> {
            check(System.nanoTime() < lightDeadline, "Lighting update timed out");
            return world.getChunkSource().getLightEngine().getLayerListener(net.minecraft.world.level.LightLayer.BLOCK).getLightValue(marker.above()) == 14;
        });
        CompletableFuture<Integer> relit = new CompletableFuture<>();
        int accepted = ((ca.spottedleaf.moonrise.patches.starlight.light.StarLightLightingProvider)world.getChunkSource().getLightEngine())
            .starlight$serverRelightChunks(List.of(pos), ignored -> {}, relit::complete);
        long relightDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        world.getChunkSource().mainThreadProcessor.managedBlock(() -> {
            check(System.nanoTime() < relightDeadline, "Native relighting timed out");
            return relit.isDone();
        });
        check(accepted == 1 && relit.join() == 1, "Native relighting did not process the requested chunk");
        ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.flush(world);
        var chunksConfig = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge) world).paperConfig().chunks;
        boolean flushRegions = chunksConfig.flushRegionsOnSave;
        chunksConfig.flushRegionsOnSave = true;
        try {
            holders.saveAllChunks(true, false, false, false);
            ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.flush(world);
        } finally {
            chunksConfig.flushRegionsOnSave = flushRegions;
        }
        holders.removeTicketAtLevel(ticket, pos, holders.ENTITY_TICKING_TICKET_LEVEL, null);
        long unloadDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        world.getChunkSource().mainThreadProcessor.managedBlock(() -> {
            check(System.nanoTime() < unloadDeadline, "Chunk holder failed to unload");
            holders.tick(); holders.processTicketUpdates(); holders.processUnloads();
            return holders.getChunkHolder(pos.pack()) == null;
        });
        check(world.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, true) instanceof LevelChunk, "Reload did not return a full chunk");
        check(world.getBlockState(marker).is(Blocks.GOLD_BLOCK), "Saved block did not survive reload");
        check(world.getPoiManager().getType(marker.east()).isPresent(), "POI did not survive reload");
        check(nativeWorld.moonrise$getEntityLookup().hasEntity(pig.getUUID()), "Saved entity did not survive reload");
        check(nativeWorld.moonrise$getEntityLookup().get(pig.getUUID()).getBukkitEntity().getEntitySpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL, "Initial spawn reason did not survive save/reload");
        var reloadedPig = nativeWorld.moonrise$getEntityLookup().get(pig.getUUID()).getBukkitEntity();
        check(reloadedPig.getOrigin().getY() == 6 && reloadedPig.fromMobSpawner() && reloadedPig.isFreezeTickingLocked()
            && reloadedPig.getVisualFire() == net.kyori.adventure.util.TriState.FALSE, "Persistent Paper entity state did not survive reload");
        attributed.verifyReload(nativeWorld.moonrise$getEntityLookup(), pig.getUUID());
        for (var uuid : daylightMobs) check(!((gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge)nativeWorld.moonrise$getEntityLookup().get(uuid)).bridge$burnInDay(), "Daylight burning override did not survive reload");
        check(((org.bukkit.entity.LivingEntity)reloadedPig).getFrictionState() == net.kyori.adventure.util.TriState.FALSE, "Living friction did not survive reload");
        var reloadedItem = (org.bukkit.entity.Item)nativeWorld.moonrise$getEntityLookup().get(friction.item()).getBukkitEntity();
        var reloadedCart = (org.bukkit.entity.Minecart)nativeWorld.moonrise$getEntityLookup().get(friction.cart()).getBukkitEntity();
        check(reloadedItem.getFrictionState() == net.kyori.adventure.util.TriState.FALSE && reloadedCart.getFrictionState() == net.kyori.adventure.util.TriState.FALSE,
            "Item or minecart friction did not survive reload");
        check(reloadedItem.canMobPickup(), "Item pickup control unexpectedly persisted instead of using Paper's default");
        var reloadedStand = (org.bukkit.entity.ArmorStand)nativeWorld.moonrise$getEntityLookup().get(armorStand).getBukkitEntity();
        check(!reloadedStand.canTick() && reloadedStand.canMove(), "Armor stand tick override or default movement did not survive reload correctly");
        check(!((org.bukkit.entity.FallingBlock)nativeWorld.moonrise$getEntityLookup().get(fallingBlock).getBukkitEntity()).doesAutoExpire(),
            "Falling block expiry override did not survive reload");
        for (var uuid : cubeMobs) check(!((org.bukkit.entity.AbstractCubeMob)nativeWorld.moonrise$getEntityLookup().get(uuid).getBukkitEntity()).canWander(),
            "Cube mob wandering override did not survive reload");
        var reloadedFurnace = (org.bukkit.block.Furnace)((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$getWorld().getBlockAt(marker.west().getX(), marker.getY(), marker.west().getZ()).getState();
        check(reloadedFurnace.getCookSpeedMultiplier() == 2, "Furnace speed multiplier did not survive save/reload");
        var reloadedBeacon = (org.bukkit.block.Beacon)((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$getWorld().getBlockAt(marker.north().getX(), marker.getY(), marker.north().getZ()).getState();
        check(reloadedBeacon.getEffectRange() == 64, "Beacon effect range did not survive save/reload");
        for (int index = 0; index < 2; ++index) {
            var sensor = (net.minecraft.world.level.block.entity.SculkSensorBlockEntity)world.getBlockEntity(marker.east(3 + index));
            check(sensor.getListener().getListenerRadius() == 24 + index * 8, "Sculk sensor listener range did not survive reload");
        }
        var reloadedMerchant = (net.minecraft.world.entity.npc.villager.Villager)nativeWorld.moonrise$getEntityLookup().get(merchant);
        check(((gr1mly4memes.felt.bridge.core.world.item.trading.MerchantOfferBridge)reloadedMerchant.getOffers().get(0)).bridge$ignoreDiscounts(),
            "Merchant ignore-discounts flag did not survive native world persistence");
        verifyLootReload(world, marker, lootFixture);
        var campfirePos = marker.north(2);
        var campfire = (org.bukkit.block.Campfire)((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$getWorld().getBlockAt(campfirePos.getX(), campfirePos.getY(), campfirePos.getZ()).getState();
        check(campfire.isCookingDisabled(0) && !campfire.isCookingDisabled(1), "Campfire suspension state did not survive reload");
        check(world.getChunkSource().getLightEngine().getLayerListener(net.minecraft.world.level.LightLayer.BLOCK).getLightValue(marker.above()) == 14, "Saved lighting did not survive reload");
        verifyPaperWorldApis(world, marker);
        if (Boolean.getBoolean("felt.verify.noise")) {
            check(!((ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemChunkStatus)(Object)ChunkStatus.BIOMES).moonrise$getWarnedAboutNoImmediateComplete().get(), "Biome generation escaped Moonrise's worker executor");
            check(!((ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemChunkStatus)(Object)ChunkStatus.TERRAIN).moonrise$getWarnedAboutNoImmediateComplete().get(), "Terrain generation escaped Moonrise's worker executor");
        }
    }

    private static void verifyEntityStorageLimits(ServerLevel world, ChunkPos pos, net.minecraft.world.entity.Entity first) {
        var second = java.util.Objects.requireNonNull(net.minecraft.world.entity.EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND));
        var sheep = java.util.Objects.requireNonNull(net.minecraft.world.entity.EntityTypes.SHEEP.create(world, EntitySpawnReason.COMMAND));
        second.setPos(first.position());
        sheep.setPos(first.position());
        var fixtures = List.of(first, second, sheep);
        var limits = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge) world).paperConfig().chunks.entityPerChunkSaveLimit;
        var prior = limits.get((Object) first.getType());
        try {
            limits.put(first.getType(), -1);
            var uncapped = ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices.saveEntityChunk(fixtures, pos, world);
            check(uncapped != null && uncapped.getListOrEmpty("Entities").size() == 3, "Unlimited entity storage dropped an entity");
            limits.put(first.getType(), 1);
            var capped = ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices.saveEntityChunk(fixtures, pos, world);
            check(capped != null && capped.getListOrEmpty("Entities").size() == 2, "Moonrise save missed the per-type entity cap");
            var read = ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices.readEntities(world, pos, uncapped);
            check(read.stream().map(net.minecraft.world.entity.Entity::getUUID).toList().equals(List.of(first.getUUID(), sheep.getUUID())),
                "Entity load missed the cap or capped unrelated types");
            limits.put(first.getType(), 0);
            var zero = ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices.readEntities(world, pos, uncapped);
            check(zero.size() == 1 && zero.getFirst().getUUID().equals(sheep.getUUID()), "Zero limit did not omit every capped entity");
        } finally {
            if (prior == null) {
                limits.removeInt(first.getType());
            } else {
                limits.put(first.getType(), prior.intValue());
            }
        }
        System.out.println("Paper entity storage limits: unlimited, one per type and zero confirmed through native save and load");
    }

    private static void verifyRedstoneImplementations(ServerLevel world, BlockPos source) {
        var misc = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge) world).paperConfig().misc;
        var prior = misc.redstoneImplementation;
        var wire = Blocks.REDSTONE_WIRE.defaultBlockState();
        int[] powerChanges = {0};
        var listeners = org.bukkit.event.block.BlockRedstoneEvent.getHandlerList();
        var registration = new org.bukkit.plugin.RegisteredListener(new org.bukkit.event.Listener() {}, (listener, raw) -> {
            var event = (org.bukkit.event.block.BlockRedstoneEvent) raw;
            if (event.getBlock().getX() == source.getX() + 1 && event.getBlock().getY() == source.getY()
                && event.getBlock().getZ() == source.getZ() && event.getNewCurrent() > event.getOldCurrent()) {
                ++powerChanges[0];
            }
        }, org.bukkit.event.EventPriority.NORMAL, verificationPlugin(), false);
        listeners.register(registration);
        try {
            for (var implementation : io.papermc.paper.configuration.WorldConfiguration.Misc.RedstoneImplementation.values()) {
                misc.redstoneImplementation = implementation;
                powerChanges[0] = 0;
                for (int x = 0; x <= 3; x++) {
                    world.setBlockAndUpdate(source.east(x).below(), Blocks.STONE.defaultBlockState());
                    world.setBlockAndUpdate(source.east(x), Blocks.AIR.defaultBlockState());
                }
                for (int x = 1; x <= 3; x++) {
                    world.setBlockAndUpdate(source.east(x), wire);
                }
                world.setBlockAndUpdate(source, Blocks.REDSTONE_BLOCK.defaultBlockState());
                for (int x = 1; x <= 3; x++) {
                    var state = world.getBlockState(source.east(x));
                    check(state.is(Blocks.REDSTONE_WIRE) && state.getValue(net.minecraft.world.level.block.RedstoneWireBlock.POWER) == 16 - x,
                        implementation + " did not propagate power to wire " + x);
                }
                check(powerChanges[0] > 0, implementation + " did not notify plugins of the wire's power change");
                world.setBlockAndUpdate(source.east(1), Blocks.AIR.defaultBlockState());
                check(world.getBlockState(source.east(3)).getValue(net.minecraft.world.level.block.RedstoneWireBlock.POWER) == 0,
                    implementation + " did not depower the remaining circuit after a wire was removed");
            }
        } finally {
            listeners.unregister(registration);
            misc.redstoneImplementation = prior;
        }
        System.out.println("Paper redstone: Vanilla, Eigencraft and Alternate Current powered, notified plugins and depowered a live circuit");
    }

    private static void verifyGrassSpreadAndFade(ServerLevel world, BlockPos source) {
        var target = source.east();
        var grass = Blocks.GRASS_BLOCK.defaultBlockState();
        world.setBlockAndUpdate(source, grass);
        world.setBlockAndUpdate(source.above(), Blocks.AIR.defaultBlockState());
        world.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
        world.setBlockAndUpdate(target.above(), Blocks.AIR.defaultBlockState());
        check(world.getMaxLocalRawBrightness(source.above()) >= 9, "Grass spread fixture did not have enough light");

        boolean[] cancel = {true};
        int[] calls = {0, 0};
        var spreadRegistration = new org.bukkit.plugin.RegisteredListener(new org.bukkit.event.Listener() {}, (listener, raw) -> {
            var event = (org.bukkit.event.block.BlockSpreadEvent) raw;
            if (event.getBlock().getX() == target.getX() && event.getBlock().getY() == target.getY()
                && event.getBlock().getZ() == target.getZ() && event.getSource().getX() == source.getX()) {
                ++calls[0];
                event.setCancelled(cancel[0]);
            }
        }, org.bukkit.event.EventPriority.NORMAL, verificationPlugin(), false);
        var fadeRegistration = new org.bukkit.plugin.RegisteredListener(new org.bukkit.event.Listener() {}, (listener, raw) -> {
            var event = (org.bukkit.event.block.BlockFadeEvent) raw;
            if (event.getBlock().getX() == source.getX() && event.getBlock().getY() == source.getY()
                && event.getBlock().getZ() == source.getZ()) {
                ++calls[1];
                event.setCancelled(cancel[0]);
            }
        }, org.bukkit.event.EventPriority.NORMAL, verificationPlugin(), false);
        var spreadHandlers = org.bukkit.event.block.BlockSpreadEvent.getHandlerList();
        var fadeHandlers = org.bukkit.event.block.BlockFadeEvent.getHandlerList();
        spreadHandlers.register(spreadRegistration);
        fadeHandlers.register(fadeRegistration);
        var eastOnly = new net.minecraft.world.level.levelgen.LegacyRandomSource(0) {
            private int axis;

            @Override public int nextInt(int bound) {
                return switch (this.axis++ % 3) {
                    case 0 -> 2;
                    case 1 -> 3;
                    default -> 1;
                };
            }
        };
        try {
            grass.randomTick(world, source, eastOnly);
            check(calls[0] > 0 && world.getBlockState(target).is(Blocks.DIRT), "Grass spread bypassed a Bukkit cancellation");
            cancel[0] = false;
            grass.randomTick(world, source, eastOnly);
            check(world.getBlockState(target).is(Blocks.GRASS_BLOCK), "Grass did not spread to available dirt");
            world.setBlockAndUpdate(source.above(), Blocks.STONE.defaultBlockState());
            cancel[0] = true;
            grass.randomTick(world, source, eastOnly);
            check(calls[1] == 1 && world.getBlockState(source).is(Blocks.GRASS_BLOCK), "Grass fade bypassed a Bukkit cancellation");
            cancel[0] = false;
            grass.randomTick(world, source, eastOnly);
            check(calls[1] == 2 && world.getBlockState(source).is(Blocks.DIRT), "Grass did not fade after the plugin allowed it");
        } finally {
            spreadHandlers.unregister(spreadRegistration);
            fadeHandlers.unregister(fadeRegistration);
        }
        System.out.println("Paper grass spreading: loaded chunk checks and Bukkit spread and fade cancellation passed");
    }

    private static void verifyPaperWorldApis(ServerLevel world, BlockPos marker) {
        var bukkit = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$getWorld();
        int oldView = bukkit.getViewDistance();
        bukkit.setViewDistance(5);
        bukkit.setSendViewDistance(3);
        check(bukkit.getSendViewDistance() == 3 && bukkit.getViewDistance() == 5, "World send distance did not remain independent from load distance");
        bukkit.setSendViewDistance(-1); bukkit.setViewDistance(oldView);
        CompletableFuture<org.bukkit.Chunk> absent = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> bukkit.getChunkAtAsync(52, -52, false, false, chunk -> {
            check(TickThread.isTickThread(), "Bukkit async chunk callback ran off the tick thread"); absent.complete(chunk);
        })).exceptionally(failure -> { absent.completeExceptionally(failure); return null; });
        await(world, absent, "Bukkit non-generating async load");
        check(absent.join() == null, "Non-generating async API generated a missing chunk");
        CompletableFuture<org.bukkit.Chunk> generated = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> bukkit.getChunkAtAsync(52, -52, true, true, chunk -> {
            check(TickThread.isTickThread(), "Urgent Bukkit callback ran off the tick thread"); generated.complete(chunk);
        })).exceptionally(failure -> { generated.completeExceptionally(failure); return null; });
        await(world, generated, "Bukkit generating async load");
        check(generated.join() != null && generated.join().getX() == 52 && generated.join().getZ() == -52, "Async load returned the wrong chunk");
        CompletableFuture<Void> area = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> bukkit.getChunksAtAsync(52, -52, 53, -52, false, () -> {
            check(TickThread.isTickThread(), "Bukkit area callback ran off the tick thread"); area.complete(null);
        })).exceptionally(failure -> { area.completeExceptionally(failure); return null; });
        await(world, area, "Bukkit async area load");
        check(world.getChunkSource().getChunkNow(53, -52) != null, "Area API missed a requested chunk");
        var cow = net.minecraft.world.entity.EntityTypes.COW.create(world, EntitySpawnReason.COMMAND);
        cow.setPos(marker.getX() + 0.5, 18, marker.getZ() + 0.5);
        check(world.addFreshEntity(cow), "Could not add API test cow");
        var apiCow = (org.bukkit.entity.Cow)cow.getBukkitEntity();
        var key = com.destroystokyo.paper.entity.ai.GoalKey.of(org.bukkit.entity.Cow.class, new org.bukkit.NamespacedKey("felt", "verification_goal"));
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var goal = new com.destroystokyo.paper.entity.ai.Goal<org.bukkit.entity.Cow>() {
            public boolean shouldActivate() { return true; }
            public void tick() { calls.incrementAndGet(); }
            public com.destroystokyo.paper.entity.ai.GoalKey<org.bukkit.entity.Cow> getKey() { return key; }
            public java.util.EnumSet<com.destroystokyo.paper.entity.ai.GoalType> getTypes() { return java.util.EnumSet.of(com.destroystokyo.paper.entity.ai.GoalType.MOVE); }
        };
        var goals = org.bukkit.Bukkit.getMobGoals();
        goals.addGoal(apiCow, 0, goal);
        check(goals.hasGoal(apiCow, key) && goals.getGoal(apiCow, key) == goal, "MobGoals did not expose the installed custom goal");
        ((gr1mly4memes.felt.bridge.core.world.entity.MobBridge)cow).bridge$goalSelector().tick();
        check(calls.get() > 0, "Custom Bukkit goal never executed in the native goal selector");
        goals.removeGoal(apiCow, key);
        check(!goals.hasGoal(apiCow, key), "MobGoals did not remove a custom goal");
        var destination = new org.bukkit.Location(bukkit, (52 << 4) + 8.5, 90, (-52 << 4) + 8.5);
        var teleport = apiCow.teleportAsync(destination);
        destination.setX(0);
        await(world, teleport, "Bukkit async teleport");
        check(teleport.join() && apiCow.getLocation().getBlockX() == (52 << 4) + 8, "Async teleport failed or used a mutated Location");
        apiCow.remove();
        boolean enabled = bukkit.isVoidDamageEnabled();
        float amount = bukkit.getVoidDamageAmount();
        double offset = bukkit.getVoidDamageMinBuildHeightOffset();
        var victim = net.minecraft.world.entity.EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND);
        victim.setPos(marker.getX() + 0.5, world.getMinY() - 1, marker.getZ() + 0.5);
        check(world.addFreshEntity(victim), "Could not add void damage test mob");
        float health = victim.getHealth();
        bukkit.setVoidDamageMinBuildHeightOffset(0);
        bukkit.setVoidDamageAmount(3);
        bukkit.setVoidDamageEnabled(false);
        victim.checkBelowWorld();
        check(victim.getHealth() == health, "Disabled world void damage still hurt a mob");
        bukkit.setVoidDamageEnabled(true);
        victim.checkBelowWorld();
        check(victim.getHealth() == health - 3, "Configured void damage amount did not reach living entity damage");
        victim.discard();
        bukkit.setVoidDamageEnabled(enabled); bukkit.setVoidDamageAmount(amount); bukkit.setVoidDamageMinBuildHeightOffset(offset);
        var difficulty = bukkit.getDifficulty();
        var peacefulZombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(world, EntitySpawnReason.COMMAND);
        peacefulZombie.setPos(marker.getX(), 18, marker.getZ());
        check(world.addFreshEntity(peacefulZombie), "Could not add peaceful despawn test zombie");
        var apiZombie = (org.bukkit.entity.Zombie)peacefulZombie.getBukkitEntity();
        apiZombie.setDespawnInPeacefulOverride(net.kyori.adventure.util.TriState.FALSE);
        bukkit.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
        peacefulZombie.checkDespawn();
        check(!peacefulZombie.isRemoved(), "FALSE peaceful override did not retain a hostile mob");
        apiZombie.setDespawnInPeacefulOverride(net.kyori.adventure.util.TriState.NOT_SET);
        peacefulZombie.checkDespawn();
        check(peacefulZombie.isRemoved(), "Clearing peaceful override did not restore hostile despawning");
        bukkit.setDifficulty(difficulty);
        System.out.println("Paper APIs: async load/no-generation, area loads, teleport, send distance, MobGoals and void damage passed");
    }

    private static java.util.List<java.util.UUID> verifyEntityControls(ServerLevel world, net.minecraft.world.entity.Mob pig, BlockPos marker) {
        var api = pig.getBukkitEntity();
        pig.setNoAi(true);
        api.setFreezeTicks(30);
        api.lockFreezeTicks(true);
        pig.aiStep();
        check(api.getFreezeTicks() == 30, "Freeze lock did not prevent vanilla freeze tick decay");
        net.minecraft.world.entity.InsideBlockEffectType.FREEZE.effect().accept(pig);
        check(api.getFreezeTicks() == 30, "Freeze lock did not prevent powder snow accumulation");
        net.minecraft.world.entity.InsideBlockEffectType.CLEAR_FREEZE.effect().accept(pig);
        check(api.getFreezeTicks() == 30, "Explicit thawing bypassed the freeze lock");
        api.lockFreezeTicks(false);
        net.minecraft.world.entity.InsideBlockEffectType.FREEZE.effect().accept(pig);
        check(api.getFreezeTicks() == 31, "Unlocking freeze ticks did not restore powder snow accumulation");
        net.minecraft.world.entity.InsideBlockEffectType.CLEAR_FREEZE.effect().accept(pig);
        check(api.getFreezeTicks() == 0, "Unlocking freeze ticks did not restore explicit thawing");
        api.setFreezeTicks(30);
        pig.setIsInPowderSnow(false);
        pig.aiStep();
        check(api.getFreezeTicks() == 28, "Unlocking freeze ticks did not restore vanilla decay");
        api.lockFreezeTicks(true);
        api.setVisualFire(net.kyori.adventure.util.TriState.FALSE);
        pig.setSharedFlagOnFire(true);
        try {
            var flags = net.minecraft.world.entity.Entity.class.getDeclaredMethod("getSharedFlag", int.class);
            flags.setAccessible(true);
            check(!(boolean)flags.invoke(pig, 0), "FALSE visual-fire override still set the client fire flag");
            api.setVisualFire(net.kyori.adventure.util.TriState.TRUE);
            pig.setSharedFlagOnFire(false);
            check((boolean)flags.invoke(pig, 0), "TRUE visual-fire override did not set the client fire flag");
            api.setVisualFire(net.kyori.adventure.util.TriState.NOT_SET);
            pig.setSharedFlagOnFire(true);
            check((boolean)flags.invoke(pig, 0), "Clearing the visual-fire override did not restore native fire flags");
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Could not inspect native client fire flags", failure); }
        api.setVisualFire(net.kyori.adventure.util.TriState.FALSE);
        ((gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge)pig).bridge$markFromSpawner();
        check(api.fromMobSpawner(), "Spawner metadata was not exposed by Bukkit");
        var uuids = new java.util.ArrayList<java.util.UUID>();
        for (var type : List.of(net.minecraft.world.entity.EntityTypes.ZOMBIE, net.minecraft.world.entity.EntityTypes.SKELETON, net.minecraft.world.entity.EntityTypes.PHANTOM)) {
            var mob = type.create(world, EntitySpawnReason.COMMAND);
            mob.setNoAi(true);
            mob.setPos(marker.getX() + 0.5, 18, marker.getZ() + 0.5);
            check(world.addFreshEntity(mob), "Could not add daylight control test mob");
            var bukkitMob = mob.getBukkitEntity();
            if (bukkitMob instanceof org.bukkit.entity.Zombie zombie) zombie.setShouldBurnInDay(false);
            else if (bukkitMob instanceof org.bukkit.entity.AbstractSkeleton skeleton) skeleton.setShouldBurnInDay(false);
            else if (bukkitMob instanceof org.bukkit.entity.Phantom phantom) phantom.setShouldBurnInDay(false);
            check(!((gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge)mob).bridge$burnInDay(), "Bukkit did not set the native daylight override");
            try {
                var sun = net.minecraft.world.entity.Mob.class.getDeclaredMethod("isSunBurnTick");
                sun.setAccessible(true);
                check(!(boolean)sun.invoke(mob), "Daylight override did not suppress native sunlight burning");
            } catch (ReflectiveOperationException failure) { throw new AssertionError("Could not check native daylight behavior", failure); }
            uuids.add(mob.getUUID());
        }
        System.out.println("Paper entity controls: origin, freeze locking, visual fire and daylight burning passed");
        return uuids;
    }

    private record LootFixture(java.util.UUID player, java.util.UUID cart, int chestItems, int cartItems) {}

    private record FrictionFixture(java.util.UUID item, java.util.UUID cart) {}

    private static void verifyAnvilEnchantmentLimit(ServerLevel world, BlockPos marker) {
        var player = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "AnvilPlayer"), net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(world.getServer(),
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        var menu = new net.minecraft.world.inventory.AnvilMenu(1, player.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.create(world, marker));
        var api = (org.bukkit.inventory.view.AnvilView)((gr1mly4memes.felt.bridge.core.world.inventory.AbstractContainerMenuBridge)menu).bridge$getBukkitView();
        check(!api.bypassesEnchantmentLevelRestriction(), "Anvils did not default to the normal enchantment limit");
        var sharpness = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
        var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        sword.enchant(sharpness, 5);
        menu.getSlot(0).set(sword.copy());
        menu.getSlot(1).set(sword.copy());
        menu.createResult();
        check(menu.getSlot(2).getItem().getEnchantments().getLevel(sharpness) == 5, "Normal anvil exceeded the enchantment cap");
        api.bypassEnchantmentLevelRestriction(true);
        menu.createResult();
        check(api.bypassesEnchantmentLevelRestriction() && menu.getSlot(2).getItem().getEnchantments().getLevel(sharpness) == 6,
            "Anvil bypass API did not permit a native level-six result");
        api.bypassEnchantmentLevelRestriction(false);
        menu.createResult();
        check(menu.getSlot(2).getItem().getEnchantments().getLevel(sharpness) == 5, "Disabling anvil bypass did not restore the cap");
        var brewPos = marker.west(2);
        world.setBlockAndUpdate(brewPos, Blocks.BREWING_STAND.defaultBlockState());
        var stand = (net.minecraft.world.level.block.entity.BrewingStandBlockEntity)world.getBlockEntity(brewPos);
        try {
            var dataField = net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class.getDeclaredField("dataAccess");
            dataField.setAccessible(true);
            var brewing = new net.minecraft.world.inventory.BrewingStandMenu(2, player.getInventory(), stand, (net.minecraft.world.inventory.ContainerData)dataField.get(stand));
            var brewingApi = (org.bukkit.inventory.view.BrewingStandView)((gr1mly4memes.felt.bridge.core.world.inventory.AbstractContainerMenuBridge)brewing).bridge$getBukkitView();
            brewingApi.setRecipeBrewTime(123);
            brewingApi.setBrewingTicks(12);
            check(stand.totalBrewTime == 123 && stand.brewTime == 12 && brewingApi.getRecipeBrewTime() == 123,
                "Brewing view duration setters did not update the live block entity data");
            boolean rejected = false;
            try { brewingApi.setRecipeBrewTime(0); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Brewing view accepted a nonpositive recipe duration");
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Could not verify native brewing container data", failure); }
        System.out.println("Paper anvils: native enchantment combining honors the view's level restriction bypass");
        System.out.println("Paper brewing views: recipe duration and remaining ticks use live block entity data");
    }

    private static java.util.UUID verifyMerchantDiscounts(ServerLevel world, BlockPos marker) {
        var villager = net.minecraft.world.entity.EntityTypes.VILLAGER.create(world, EntitySpawnReason.COMMAND);
        villager.setPos(marker.getX() + 0.5, world.getMaxY() - 20, marker.getZ() + 0.5);
        villager.setNoGravity(true);
        villager.setNoAi(true);
        check(world.addFreshEntity(villager), "Could not add merchant discount fixture");
        var player = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "DiscountPlayer"), net.minecraft.server.level.ClientInformation.createDefault());
        player.activeEffects.put(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE,
            new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE, 200, 0));
        villager.getGossips().add(player.getUUID(), net.minecraft.world.entity.ai.gossip.GossipType.MAJOR_POSITIVE, 20);
        var offers = new net.minecraft.world.item.trading.MerchantOffers();
        for (boolean ignore : new boolean[] {true, false}) {
            var recipe = new org.bukkit.craftbukkit.inventory.CraftMerchantRecipe(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BREAD),
                0, 10, true, 1, 0.2F, ignore);
            recipe.addIngredient(new org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD, 10));
            check(recipe.shouldIgnoreDiscounts() == ignore, "Merchant recipe constructor ignored its discount setting");
            offers.add(recipe.toMinecraft());
        }
        villager.setOffers(offers);
        villager.updateSpecialPrices(player);
        check(offers.get(0).getSpecialPriceDiff() == 0 && offers.get(1).getSpecialPriceDiff() < 0,
            "Native villager reputation/hero pricing ignored the per-offer discount flag");
        var recipe = ((gr1mly4memes.felt.bridge.core.world.item.trading.MerchantOfferBridge)offers.get(0)).bridge$asBukkit();
        recipe.setIgnoreDiscounts(false);
        villager.updateSpecialPrices(player);
        check(offers.get(0).getSpecialPriceDiff() < 0, "Re-enabling merchant discounts did not restore native pricing");
        recipe.setIgnoreDiscounts(true);
        check(((gr1mly4memes.felt.bridge.core.world.item.trading.MerchantOfferBridge)offers.get(0).copy()).bridge$ignoreDiscounts(),
            "Copying a merchant offer lost its discount setting");
        var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, world.registryAccess());
        var encoded = net.minecraft.world.item.trading.MerchantOffer.CODEC.encodeStart(ops, offers.get(0)).getOrThrow();
        var decoded = net.minecraft.world.item.trading.MerchantOffer.CODEC.parse(ops, encoded).getOrThrow();
        check(((gr1mly4memes.felt.bridge.core.world.item.trading.MerchantOfferBridge)decoded).bridge$ignoreDiscounts(), "Merchant codec lost its Paper flag");
        var defaultEncoded = net.minecraft.world.item.trading.MerchantOffer.CODEC.encodeStart(ops, offers.get(1)).getOrThrow();
        check(!((gr1mly4memes.felt.bridge.core.world.item.trading.MerchantOfferBridge)net.minecraft.world.item.trading.MerchantOffer.CODEC.parse(ops, defaultEncoded).getOrThrow()).bridge$ignoreDiscounts(),
            "A vanilla merchant offer without the flag no longer defaults to discounts");
        System.out.println("Paper merchants: ignore-discount recipes, reputation/hero pricing, offer copying and native codec passed");
        return villager.getUUID();
    }

    private static void verifyWitchDrinking(ServerLevel world, BlockPos marker) {
        var witch = net.minecraft.world.entity.EntityTypes.WITCH.create(world, EntitySpawnReason.COMMAND);
        witch.setPos(marker.getX() + 0.5, world.getMaxY() - 20, marker.getZ() + 0.5);
        witch.setNoGravity(true);
        witch.setSilent(true);
        check(world.addFreshEntity(witch), "Could not add witch drinking fixture");
        var api = (org.bukkit.entity.Witch)witch.getBukkitEntity();
        var potion = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, net.minecraft.world.item.alchemy.Potions.FIRE_RESISTANCE);
        double normalSpeed = witch.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var count = new java.util.concurrent.atomic.AtomicInteger();
        var cancel = new java.util.concurrent.atomic.AtomicBoolean();
        var handler = new org.bukkit.plugin.RegisteredListener(new org.bukkit.event.Listener() {}, (listener, event) -> {
            var ready = (com.destroystokyo.paper.event.entity.WitchReadyPotionEvent)event;
            if (ready.getEntity().getUniqueId().equals(witch.getUUID())) { count.incrementAndGet(); ready.setCancelled(cancel.get()); }
        }, org.bukkit.event.EventPriority.NORMAL, verificationPlugin(), false);
        var handlers = com.destroystokyo.paper.event.entity.WitchReadyPotionEvent.getHandlerList();
        handlers.register(handler);
        try {
            api.setDrinkingPotion(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(potion));
            check(count.get() == 1 && api.isDrinkingPotion() && api.getPotionUseTimeLeft() > 0 && !witch.getMainHandItem().isEmpty(),
                "Witch API did not start native potion drinking or dispatch one ready event");
            check(witch.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) < normalSpeed,
                "Witch API did not apply the drinking movement penalty");
            api.setPotionUseTimeLeft(0);
            witch.aiStep();
            check(!api.isDrinkingPotion() && witch.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)
                && witch.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == normalSpeed,
                "Witch did not consume the API potion and restore normal movement");
            cancel.set(true);
            api.setDrinkingPotion(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(potion));
            check(count.get() == 2 && witch.getMainHandItem().isEmpty(), "Cancelled witch ready event still equipped a potion");
            boolean rejected = false;
            try { api.setDrinkingPotion(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE)); }
            catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Witch API accepted a non-potion item");
        } finally { handlers.unregister(handler); witch.discard(); }
        System.out.println("Paper witches: API drinking, ready event cancellation, consumption and movement penalty passed");
    }

    private static java.util.List<java.util.UUID> verifyCubeMobControls(ServerLevel world, BlockPos marker) {
        var retained = new java.util.ArrayList<java.util.UUID>();
        for (var type : java.util.List.of(net.minecraft.world.entity.EntityTypes.SLIME, net.minecraft.world.entity.EntityTypes.MAGMA_CUBE)) {
            var cube = type.create(world, EntitySpawnReason.COMMAND);
            cube.setPos(marker.getX() + 0.5, world.getMaxY() - 10, marker.getZ() + 0.5);
            cube.setNoGravity(true);
            cube.setOnGround(true);
            check(world.addFreshEntity(cube), "Could not add cube mob control fixture");
            var api = (org.bukkit.entity.AbstractCubeMob)cube.getBukkitEntity();
            check(api.canWander(), "Cube mobs did not default to wandering");
            try {
                var wander = cubeGoal("CubeMobKeepOnJumpingGoal", cube);
                var direction = cubeGoal("CubeMobRandomDirectionGoal", cube);
                check(wander.canUse() && direction.canUse(), "Default cube mob wandering goals were blocked");
                api.setWander(false);
                check(!wander.canUse() && !direction.canUse(), "Disabling wandering did not block idle cube mob goals");
                var target = net.minecraft.world.entity.EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND);
                cube.setTarget(target);
                var attack = cubeGoal("CubeMobAttackGoal", cube);
                check(!attack.canUse(), "A cube mob attacked while wandering was disabled");
                api.setWander(true);
                check(attack.canUse(), "Re-enabling wandering did not restore cube mob targeting");
                attack.start();
                check(attack.canContinueToUse(), "Enabled cube mob attack did not continue");
                api.setWander(false);
                check(!attack.canContinueToUse(), "Disabling wandering did not stop an active cube mob attack");
                attack.stop();
                check(cube.getTarget() == null, "Stopping cube mob attack did not clear its target");
            } catch (ReflectiveOperationException failure) { throw new AssertionError("Could not construct native cube mob goals", failure); }
            retained.add(cube.getUUID());
        }
        System.out.println("Paper cube mobs: slime and magma cube wandering, attack gating and target cleanup passed");
        return retained;
    }

    private static net.minecraft.world.entity.ai.goal.Goal cubeGoal(String name, net.minecraft.world.entity.monster.cubemob.AbstractCubeMob cube) throws ReflectiveOperationException {
        var constructor = Class.forName("net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$" + name).getDeclaredConstructor(net.minecraft.world.entity.monster.cubemob.AbstractCubeMob.class);
        constructor.setAccessible(true);
        return (net.minecraft.world.entity.ai.goal.Goal)constructor.newInstance(cube);
    }

    private static java.util.UUID verifyFallingBlockControls(ServerLevel world, BlockPos marker) {
        var block = net.minecraft.world.entity.EntityTypes.FALLING_BLOCK.create(world, EntitySpawnReason.COMMAND);
        block.blockState = Blocks.SAND.defaultBlockState();
        block.setNoGravity(true);
        block.dropItem = false;
        block.setPos(marker.getX() + 0.5, world.getMaxY() - 15, marker.getZ() + 0.5);
        check(world.addFreshEntity(block), "Could not add falling block expiry fixture");
        var api = (org.bukkit.entity.FallingBlock)block.getBukkitEntity();
        check(api.doesAutoExpire(), "Falling blocks did not default to expiring");
        api.shouldAutoExpire(false);
        api.setTicksLived(601);
        block.tick();
        check(!block.isRemoved() && block.time > 601, "Disabling falling block expiry stopped its age or failed to prevent expiry");
        block.setPos(block.getX(), world.getMaxY() + 1, block.getZ());
        api.setTicksLived(101);
        block.tick();
        check(!block.isRemoved(), "A non-expiring falling block expired above the world");
        api.shouldAutoExpire(true);
        block.tick();
        check(block.isRemoved(), "Re-enabling falling block expiry did not restore the out-of-world timeout");
        var retained = net.minecraft.world.entity.EntityTypes.FALLING_BLOCK.create(world, EntitySpawnReason.COMMAND);
        retained.blockState = Blocks.SAND.defaultBlockState();
        retained.setNoGravity(true);
        retained.setPos(marker.getX() + 0.5, world.getMaxY() - 15, marker.getZ() + 0.5);
        check(world.addFreshEntity(retained), "Could not add persistent falling block fixture");
        ((org.bukkit.entity.FallingBlock)retained.getBukkitEntity()).shouldAutoExpire(false);
        var fixes = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$paperConfig().fixes;
        var heightLimit = fixes.fallingBlockHeightNerf;
        var limited = net.minecraft.world.entity.EntityTypes.FALLING_BLOCK.create(world, EntitySpawnReason.COMMAND);
        limited.blockState = Blocks.SAND.defaultBlockState();
        limited.dropItem = false;
        limited.setNoGravity(true);
        limited.setPos(retained.position());
        check(world.addFreshEntity(limited), "Could not add falling block height-limit fixture");
        ((org.bukkit.entity.FallingBlock)limited.getBukkitEntity()).shouldAutoExpire(false);
        try {
            fixes.fallingBlockHeightNerf = new io.papermc.paper.configuration.type.number.IntOr.Disabled(java.util.OptionalInt.of(world.getMaxY() - 16));
            limited.tick();
            check(limited.isRemoved(), "The configured falling block height limit was bypassed by disabling age expiry");
        } finally { fixes.fallingBlockHeightNerf = heightLimit; limited.discard(); }
        System.out.println("Paper falling blocks: expiry controls, normal aging and out-of-world timeout restoration passed");
        return retained.getUUID();
    }

    private static java.util.UUID verifyArmorStandControls(ServerLevel world, BlockPos marker) {
        var config = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$paperConfig().entities.armorStands;
        boolean defaultTick = config.tick;
        config.tick = false;
        var configured = net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(world, EntitySpawnReason.COMMAND);
        check(!((org.bukkit.entity.ArmorStand)configured.getBukkitEntity()).canTick(), "Armor stand construction ignored its world tick setting");
        config.tick = defaultTick;
        var stand = net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(world, EntitySpawnReason.COMMAND);
        stand.setPos(marker.getX() + 0.5, world.getMaxY() - 20, marker.getZ() + 0.5);
        stand.setNoGravity(true);
        check(world.addFreshEntity(stand), "Could not add armor stand control fixture");
        var api = (org.bukkit.entity.ArmorStand)stand.getBukkitEntity();
        var start = stand.position();
        api.setCanMove(false);
        stand.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1, 0, 0));
        check(stand.position().equals(start), "Disabled armor stand movement still changed position");
        api.setCanMove(true);
        stand.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1, 0, 0));
        check(stand.getX() > start.x, "Re-enabled armor stand movement did not change position");
        api.setCanTick(false);
        stand.setRemainingFireTicks(10);
        api.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HELMET));
        stand.tick();
        check(stand.getRemainingFireTicks() == 10, "Disabled armor stand ticking still ran its normal base tick");
        try {
            var cacheField = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("lastEquipmentItems");
            cacheField.setAccessible(true);
            var cache = (java.util.Map<?, ?>)cacheField.get(stand);
            check(cache.get(net.minecraft.world.entity.EquipmentSlot.HEAD) instanceof net.minecraft.world.item.ItemStack helmet
                && helmet.is(net.minecraft.world.item.Items.DIAMOND_HELMET), "Non-ticking armor stand did not synchronize changed equipment");
            var cart = net.minecraft.world.entity.EntityTypes.MINECART.create(world, EntitySpawnReason.COMMAND);
            cart.setNoGravity(true);
            cart.setPos(stand.getX() + 0.2, stand.getY(), stand.getZ());
            check(world.addFreshEntity(cart), "Could not add armor stand collision fixture");
            var push = net.minecraft.world.entity.decoration.ArmorStand.class.getDeclaredMethod("pushEntities");
            push.setAccessible(true);
            boolean lookup = config.doCollisionEntityLookups;
            try {
                config.doCollisionEntityLookups = false;
                cart.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                push.invoke(stand);
                check(cart.getDeltaMovement().lengthSqr() == 0, "Disabled armor stand collision lookup still pushed a minecart");
                config.doCollisionEntityLookups = true;
                push.invoke(stand);
                check(cart.getDeltaMovement().lengthSqr() > 0, "Enabled armor stand collision lookup did not push a nearby minecart");
            } finally { config.doCollisionEntityLookups = lookup; cart.discard(); }
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Could not verify armor stand equipment or collisions", failure); }
        api.setCanTick(true);
        stand.tick();
        check(stand.getRemainingFireTicks() < 10, "Re-enabled armor stand ticking did not restore base ticks");
        stand.setRemainingFireTicks(0);
        api.setCanTick(false);
        api.setCanMove(false);
        var dying = net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(world, EntitySpawnReason.COMMAND);
        dying.setPos(stand.getX() + 2, stand.getY(), stand.getZ());
        check(world.addFreshEntity(dying), "Could not add non-ticking death fixture");
        ((org.bukkit.entity.ArmorStand)dying.getBukkitEntity()).setCanTick(false);
        dying.setHealth(0);
        for (int ticks = 0; ticks < 25 && !dying.isRemoved(); ++ticks) dying.tick();
        check(dying.isRemoved(), "A dead non-ticking armor stand remained permanently registered");
        System.out.println("Paper armor stands: movement, ticking, equipment sync, collision settings and death progression passed");
        return stand.getUUID();
    }

    private static void verifySpawningInfluence(ServerLevel world, BlockPos marker) {
        var near = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "InfluenceNear"), net.minecraft.server.level.ClientInformation.createDefault());
        var far = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "InfluenceFar"), net.minecraft.server.level.ClientInformation.createDefault());
        BlockPos spawnPos = marker.above();
        world.setBlockAndUpdate(spawnPos, Blocks.AIR.defaultBlockState());
        near.setPos(marker.getX() + 0.5, spawnPos.getY(), marker.getZ() + 0.5);
        far.setPos(marker.getX() + 1600, spawnPos.getY(), marker.getZ());
        world.players().add(near); world.players().add(far);
        try {
            var api = near.getBukkitEntity();
            check(api.getAffectsSpawning(), "Players did not default to affecting spawning");
            var spawner = new net.minecraft.world.level.BaseSpawner() {
                public void broadcastEvent(net.minecraft.world.level.Level level, BlockPos pos, int event) {}
            };
            var horse = net.minecraft.world.entity.EntityTypes.SKELETON_HORSE.create(world, EntitySpawnReason.COMMAND);
            horse.setPos(near.getX(), near.getY(), near.getZ());
            var trap = new net.minecraft.world.entity.animal.equine.SkeletonTrapGoal(horse);
            var detector = net.minecraft.world.level.block.entity.trialspawner.PlayerDetector.NO_CREATIVE_PLAYERS;
            var selector = net.minecraft.world.level.block.entity.trialspawner.PlayerDetector.EntitySelector.onlySelectPlayers(java.util.List.of(near, far));
            var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(world, EntitySpawnReason.COMMAND);
            zombie.setNoAi(true);
            zombie.setPos(near.getX(), near.getY(), near.getZ());
            check(world.addFreshEntity(zombie), "Could not add spawning influence test mob");
            try {
                var isNear = net.minecraft.world.level.BaseSpawner.class.getDeclaredMethod("isNearPlayer", net.minecraft.world.level.Level.class, BlockPos.class);
                isNear.setAccessible(true);
                check((boolean)isNear.invoke(spawner, world, marker), "An influencing player did not activate a spawner");
                check(trap.canUse(), "An influencing player did not activate a skeleton trap");
                check(detector.detect(world, selector, marker, 16, false).contains(near.getUUID()), "An influencing player was excluded from a trial spawner");
                check(!net.minecraft.world.entity.monster.Endermite.checkEndermiteSpawnRules(net.minecraft.world.entity.EntityTypes.ENDERMITE, world, EntitySpawnReason.NATURAL, spawnPos, world.getRandom()),
                    "An influencing nearby player did not prevent endermite spawning");
                zombie.checkDespawn();
                check(!zombie.isRemoved(), "A mob despawned despite an influencing nearby player");
                api.setAffectsSpawning(false);
                check(!api.getAffectsSpawning(), "Player influence API did not update native state");
                check(!(boolean)isNear.invoke(spawner, world, marker), "A non-influencing player still activated a spawner");
                check(!trap.canUse(), "A non-influencing player still activated a skeleton trap");
                check(!detector.detect(world, selector, marker, 16, false).contains(near.getUUID()), "A non-influencing player still activated a trial spawner");
                check(net.minecraft.world.entity.monster.Endermite.checkEndermiteSpawnRules(net.minecraft.world.entity.EntityTypes.ENDERMITE, world, EntitySpawnReason.NATURAL, spawnPos, world.getRandom())
                    && net.minecraft.world.entity.monster.Silverfish.checkSilverfishSpawnRules(net.minecraft.world.entity.EntityTypes.SILVERFISH, world, EntitySpawnReason.NATURAL, spawnPos, world.getRandom()),
                    "A non-influencing player still prevented small-monster spawning");
                zombie.checkDespawn();
                check(zombie.isRemoved(), "A non-influencing player prevented distance-based mob despawning");
                api.setAffectsSpawning(true);
                check((boolean)isNear.invoke(spawner, world, marker) && trap.canUse(), "Re-enabling player influence did not restore spawning checks");
            } catch (ReflectiveOperationException failure) { throw new AssertionError("Could not exercise native spawner activation", failure); }
            finally { if (!zombie.isRemoved()) zombie.discard(); }
        } finally { world.players().remove(near); world.players().remove(far); }
        System.out.println("Paper player influence: native spawners, traps, trial detection, small-monster rules and mob despawning passed");
    }

    private static FrictionFixture verifyFrictionControls(ServerLevel world, net.minecraft.world.entity.Mob pig, BlockPos marker) {
        var api = (org.bukkit.entity.LivingEntity)pig.getBukkitEntity();
        pig.setDiscardFriction(true);
        api.setFrictionState(net.kyori.adventure.util.TriState.NOT_SET);
        check(pig.shouldDiscardFriction(), "Default living friction did not preserve native discard state");
        api.setFrictionState(net.kyori.adventure.util.TriState.TRUE);
        check(!pig.shouldDiscardFriction(), "Forced living friction did not override native discard state");
        api.setFrictionState(net.kyori.adventure.util.TriState.FALSE);
        check(pig.shouldDiscardFriction(), "Disabled living friction still applied native drag");
        pig.setDiscardFriction(false);
        var item = new net.minecraft.world.entity.item.ItemEntity(world, marker.getX() + 0.5, world.getMaxY() - 20, marker.getZ() + 0.5,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
        item.setNoGravity(true);
        check(world.addFreshEntity(item), "Could not add friction test item");
        var apiItem = (org.bukkit.entity.Item)item.getBukkitEntity();
        apiItem.setCanMobPickup(false);
        apiItem.setFrictionState(net.kyori.adventure.util.TriState.FALSE);
        item.setDeltaMovement(1, 0, 0);
        item.tick();
        check(Math.abs(item.getDeltaMovement().x - 1.0) < 1.0E-6, "Disabled item friction reduced horizontal motion");
        apiItem.setFrictionState(net.kyori.adventure.util.TriState.TRUE);
        item.setDeltaMovement(1, 0, 0);
        item.tick();
        check(item.getDeltaMovement().x > 0 && item.getDeltaMovement().x < 1, "Enabled item friction did not reduce horizontal motion");
        apiItem.setFrictionState(net.kyori.adventure.util.TriState.NOT_SET);
        item.setDeltaMovement(1, 0, 0);
        item.tick();
        check(item.getDeltaMovement().x > 0 && item.getDeltaMovement().x < 1, "Cleared item friction did not restore native drag");
        apiItem.setFrictionState(net.kyori.adventure.util.TriState.FALSE);
        var cart = net.minecraft.world.entity.EntityTypes.MINECART.create(world, EntitySpawnReason.COMMAND);
        cart.setPos(marker.getX() + 0.5, 18, marker.getZ() + 0.5);
        check(world.addFreshEntity(cart), "Could not add friction test minecart");
        var apiCart = (org.bukkit.entity.Minecart)cart.getBukkitEntity();
        var oldBehavior = new net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior(cart);
        var newBehavior = new net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior(cart);
        apiCart.setFrictionState(net.kyori.adventure.util.TriState.FALSE);
        check(oldBehavior.getSlowdownFactor() == 1 && newBehavior.getSlowdownFactor() == 1, "Minecart friction override did not affect both behavior implementations");
        apiCart.setFrictionState(net.kyori.adventure.util.TriState.TRUE);
        check(oldBehavior.getSlowdownFactor() < 1 && newBehavior.getSlowdownFactor() < 1, "Enabled minecart friction did not restore slowdown");
        apiCart.setFrictionState(net.kyori.adventure.util.TriState.FALSE);
        var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(world, EntitySpawnReason.COMMAND);
        zombie.setPos(marker.getX() + 0.5, world.getMaxY() - 20, marker.getZ() + 0.5);
        zombie.setNoGravity(true);
        zombie.setCanPickUpLoot(true);
        ((gr1mly4memes.felt.bridge.core.world.entity.MobBridge)zombie).bridge$setAware(false);
        check(world.addFreshEntity(zombie), "Could not add pickup test zombie");
        var sword = new net.minecraft.world.entity.item.ItemEntity(world, zombie.getX(), zombie.getY(), zombie.getZ(),
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        sword.setNoGravity(true);
        check(world.addFreshEntity(sword), "Could not add pickup test item");
        var apiSword = (org.bukkit.entity.Item)sword.getBukkitEntity();
        apiSword.setCanMobPickup(false);
        zombie.aiStep();
        check(!sword.isRemoved() && zombie.getMainHandItem().isEmpty(), "Mob picked up an item while pickup was disabled");
        apiSword.setCanMobPickup(true);
        zombie.aiStep();
        check(sword.isRemoved() && zombie.getMainHandItem().is(net.minecraft.world.item.Items.IRON_SWORD), "Re-enabled mob pickup did not equip the item");
        zombie.discard();
        System.out.println("Paper friction and pickup: native living/item/cart behavior, both minecart engines and mob equipment pickup passed");
        return new FrictionFixture(item.getUUID(), cart.getUUID());
    }

    private static LootFixture verifyLootReplenishment(ServerLevel world, BlockPos marker) {
        var config = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$paperConfig().lootables;
        boolean previous = config.autoReplenish;
        config.autoReplenish = true;
        try {
            var table = net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, net.minecraft.resources.Identifier.parse("minecraft:chests/simple_dungeon"));
            BlockPos pos = marker.south();
            world.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            var container = (net.minecraft.world.level.block.entity.ChestBlockEntity)world.getBlockEntity(pos);
            container.setLootTable(table, 42L);
            container.unpackLootTable(null);
            var bukkit = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$getWorld();
            var api = (com.destroystokyo.paper.loottable.LootableInventory)bukkit.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getState(false);
            check(api.isRefillEnabled() && api.hasBeenFilled() && api.hasPendingRefill() && container.getLootTable() != null,
                "First loot fill did not retain its table and schedule replenishment");
            check(containerItems(container) > 0, "First loot fill did not produce items");
            var player = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "LootTester"), net.minecraft.server.level.ClientInformation.createDefault());
            container.clearContent();
            api.setNextRefill(0);
            container.unpackLootTable(player);
            check(api.hasPlayerLooted(player.getUUID()) && !api.canPlayerLoot(player.getUUID()) && api.getLastLooted(player.getUUID()) != null,
                "Loot refill did not record player loot history");
            api.setNextRefill(0);
            container.unpackLootTable(player);
            check(api.getNextRefill() == 0, "Player reloot restriction did not prevent replenishment");
            check(api.setHasPlayerLooted(player.getUUID(), false), "Clearing loot history did not return its previous state");
            container.clearContent();
            var handlers = com.destroystokyo.paper.loottable.LootableInventoryReplenishEvent.getHandlerList();
            var cancel = new org.bukkit.plugin.RegisteredListener(new org.bukkit.event.Listener() {}, (listener, event) ->
                ((com.destroystokyo.paper.loottable.LootableInventoryReplenishEvent)event).setCancelled(true),
                org.bukkit.event.EventPriority.NORMAL, verificationPlugin(), false);
            handlers.register(cancel);
            try { container.unpackLootTable(player); }
            finally { handlers.unregister(cancel); }
            check(containerItems(container) == 0 && !api.hasPlayerLooted(player.getUUID()), "Cancelled loot replenish event still filled or changed player history");
            container.unpackLootTable(player);
            check(containerItems(container) > 0 && api.hasPlayerLooted(player.getUUID()), "Uncancelled loot replenish did not fill and track the player");
            var cart = net.minecraft.world.entity.EntityTypes.CHEST_MINECART.create(world, EntitySpawnReason.COMMAND);
            cart.setPos(marker.getX() + 0.5, 18, marker.getZ() + 0.5);
            check(world.addFreshEntity(cart), "Could not add lootable test vehicle");
            var vehicle = (net.minecraft.world.entity.vehicle.ContainerEntity)cart;
            vehicle.setContainerLootTable(table);
            vehicle.setContainerLootTableSeed(42L);
            vehicle.unpackChestVehicleLootTable(player);
            var cartApi = (com.destroystokyo.paper.loottable.LootableInventory)cart.getBukkitEntity();
            check(cartApi.hasBeenFilled() && cartApi.hasPlayerLooted(player.getUUID()) && vehicle.getContainerLootTable() != null,
                "Vehicle loot refill did not retain its table or track the player");
            BlockPos shulkerPos = marker.south(2);
            world.setBlockAndUpdate(shulkerPos, Blocks.SHULKER_BOX.defaultBlockState());
            var shulker = (net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity)world.getBlockEntity(shulkerPos);
            shulker.setLootTable(table, 42L);
            shulker.unpackLootTable(null);
            var drops = bukkit.getBlockAt(shulkerPos.getX(), shulkerPos.getY(), shulkerPos.getZ()).getDrops();
            check(!drops.isEmpty(), "Lootable shulker did not produce a drop");
            for (var drop : drops) if (drop.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta meta
                && meta.getBlockState() instanceof org.bukkit.block.ShulkerBox box)
                check(box.getLootTable() == null, "Used shulker drop retained a replenishable loot table");
            check(shulker.getLootTable() != null, "Drop calculation did not restore the original shulker loot table");
            BlockPos vanillaPos = marker.south(3);
            world.setBlockAndUpdate(vanillaPos, Blocks.CHEST.defaultBlockState());
            var vanilla = (net.minecraft.world.level.block.entity.ChestBlockEntity)world.getBlockEntity(vanillaPos);
            vanilla.setLootTable(table, 42L);
            config.autoReplenish = false;
            vanilla.unpackLootTable(null);
            check(vanilla.getLootTable() == null, "Disabled replenishment did not restore one-shot vanilla loot");
            config.autoReplenish = true;
            System.out.println("Paper lootables: block and vehicle fills, player history, reloot restriction, cancellation, shulker drops and disabled mode passed");
            return new LootFixture(player.getUUID(), cart.getUUID(), containerItems(container), containerItems((net.minecraft.world.Container)vehicle));
        } finally { config.autoReplenish = previous; }
    }

    private static void verifyLootReload(ServerLevel world, BlockPos marker, LootFixture fixture) {
        var config = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$paperConfig().lootables;
        boolean previous = config.autoReplenish;
        config.autoReplenish = true;
        try {
            BlockPos pos = marker.south();
            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)world.getBlockEntity(pos);
            var api = (com.destroystokyo.paper.loottable.LootableInventory)((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getState(false);
            check(chest.getLootTable() != null && api.hasPlayerLooted(fixture.player()) && containerItems(chest) == fixture.chestItems(),
                "Block loot items, table or player history did not survive reload");
            var cart = ((ChunkSystemServerLevel)world).moonrise$getEntityLookup().get(fixture.cart());
            check(cart != null && ((com.destroystokyo.paper.loottable.LootableInventory)cart.getBukkitEntity()).hasPlayerLooted(fixture.player())
                && containerItems((net.minecraft.world.Container)cart) == fixture.cartItems(), "Vehicle loot items or player history did not survive reload");
        } finally { config.autoReplenish = previous; }
    }

    private static int containerItems(net.minecraft.world.Container container) {
        int items = 0;
        for (int slot = 0; slot < container.getContainerSize(); ++slot) items += container.getItem(slot).getCount();
        return items;
    }

    private static void verifyBlockControls(ServerLevel world, BlockPos marker) {
        var bukkit = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$getWorld();
        BlockPos furnacePos = marker.west();
        world.setBlockAndUpdate(furnacePos, Blocks.FURNACE.defaultBlockState());
        var furnace = (org.bukkit.block.Furnace)bukkit.getBlockAt(furnacePos.getX(), furnacePos.getY(), furnacePos.getZ()).getState();
        furnace.getSnapshotInventory().setItem(0, new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_ORE));
        furnace.getSnapshotInventory().setItem(1, new org.bukkit.inventory.ItemStack(org.bukkit.Material.COAL));
        furnace.setCookSpeedMultiplier(2);
        check(furnace.getCookSpeedMultiplier() == 2 && furnace.getCookTimeTotal() == 100, "Furnace snapshot did not apply its speed multiplier");
        check(furnace.update(true, false), "Could not apply furnace snapshot");
        net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(world, furnacePos, world.getBlockState(furnacePos),
            (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)world.getBlockEntity(furnacePos));
        var active = (org.bukkit.block.Furnace)bukkit.getBlockAt(furnacePos.getX(), furnacePos.getY(), furnacePos.getZ()).getState();
        check(active.getCookTimeTotal() == 100 && active.getCookTime() > 0, "Native furnace ticking ignored the multiplier");
        BlockPos beaconPos = marker.north();
        world.setBlockAndUpdate(beaconPos, Blocks.BEACON.defaultBlockState());
        var beacon = (org.bukkit.block.Beacon)bukkit.getBlockAt(beaconPos.getX(), beaconPos.getY(), beaconPos.getZ()).getState();
        beacon.setEffectRange(64);
        beacon.setPrimaryEffect(org.bukkit.potion.PotionEffectType.SPEED);
        check(beacon.update(true, false), "Could not apply beacon snapshot");
        check(((gr1mly4memes.felt.bridge.core.world.level.block.entity.BeaconBlockEntityBridge)world.getBlockEntity(beaconPos)).bridge$effectRange() == 64, "Live beacon did not receive its custom range");
        try {
            var effects = net.minecraft.world.level.block.entity.BeaconBlockEntity.class.getDeclaredMethod("applyEffects", net.minecraft.world.level.Level.class,
                BlockPos.class, int.class, net.minecraft.core.Holder.class, net.minecraft.core.Holder.class);
            effects.setAccessible(true);
            effects.invoke(null, world, beaconPos, 1, org.bukkit.craftbukkit.potion.CraftPotionEffectType.bukkitToMinecraftHolder(org.bukkit.potion.PotionEffectType.SPEED), null);
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Custom beacon range failed in the native effect path", failure); }
        beacon.resetEffectRange();
        check(beacon.getEffectRange() == beacon.getTier() * 10 + 10, "Reset beacon range did not restore the tier default");
        BlockPos campfirePos = marker.north(2);
        world.setBlockAndUpdate(campfirePos, Blocks.CAMPFIRE.defaultBlockState());
        var campfire = (org.bukkit.block.Campfire)bukkit.getBlockAt(campfirePos.getX(), campfirePos.getY(), campfirePos.getZ()).getState();
        for (int slot = 0; slot < 2; ++slot) {
            campfire.setItem(slot, new org.bukkit.inventory.ItemStack(org.bukkit.Material.PORKCHOP));
            campfire.setCookTimeTotal(slot, 100);
        }
        check(!campfire.stopCooking(0) && campfire.stopCooking(0), "Campfire stop API did not return the prior state");
        check(campfire.startCooking(0) && !campfire.isCookingDisabled(0), "Campfire start API did not resume a stopped slot");
        campfire.stopCooking();
        check(campfire.isCookingDisabled(0) && campfire.isCookingDisabled(3), "Campfire stop-all did not stop every slot");
        campfire.startCooking();
        check(!campfire.isCookingDisabled(0) && !campfire.isCookingDisabled(3), "Campfire start-all did not resume every slot");
        campfire.stopCooking(0);
        check(campfire.update(true, false), "Could not apply campfire controls");
        net.minecraft.world.level.block.entity.CampfireBlockEntity.cookTick(world, campfirePos, world.getBlockState(campfirePos),
            (net.minecraft.world.level.block.entity.CampfireBlockEntity)world.getBlockEntity(campfirePos),
            net.minecraft.world.item.crafting.RecipeManager.createCheck(net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING));
        var tickingCampfire = (org.bukkit.block.Campfire)bukkit.getBlockAt(campfirePos.getX(), campfirePos.getY(), campfirePos.getZ()).getState();
        check(tickingCampfire.getCookTime(0) == 0 && tickingCampfire.getCookTime(1) == 1, "Native campfire ticking ignored per-slot suspension: " + tickingCampfire.getCookTime(0) + ", " + tickingCampfire.getCookTime(1));
        for (int index = 0; index < 2; ++index) {
            var sensorPos = marker.east(3 + index);
            world.setBlockAndUpdate(sensorPos, (index == 0 ? Blocks.SCULK_SENSOR : Blocks.CALIBRATED_SCULK_SENSOR).defaultBlockState());
            var sensor = (org.bukkit.block.SculkSensor)bukkit.getBlockAt(sensorPos.getX(), sensorPos.getY(), sensorPos.getZ()).getState();
            check(sensor.getListenerRange() == (index == 0 ? 8 : 16), "Sculk sensor did not retain its native default range");
            sensor.setListenerRange(24 + index * 8);
            check(sensor.getListenerRange() == 24 + index * 8 && sensor.update(true, false), "Could not apply sculk sensor range snapshot");
            check(((net.minecraft.world.level.block.entity.SculkSensorBlockEntity)world.getBlockEntity(sensorPos)).getListener().getListenerRadius() == 24 + index * 8,
                "The native sculk vibration listener ignored its API override");
            boolean rejected = false;
            try { sensor.setListenerRange(0); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Sculk sensor API accepted a nonpositive listener range");
        }
        System.out.println("Paper block controls: furnace snapshot and native cooking, beacon range and native effect dispatch passed");
        System.out.println("Paper sculk sensors: ordinary and calibrated native listener ranges and block snapshots passed");
    }

    private static void await(ServerLevel world, CompletableFuture<?> future, String operation) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        world.getServer().managedBlock(() -> {
            ((gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge)world.getServer()).bridge$drainQueuedTasks();
            check(System.nanoTime() < deadline, operation + " timed out");
            return future.isDone();
        });
        future.join();
    }

    private static void verifySpawnAccounting(ServerLevel world, BlockPos marker) {
        var spawning = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$paperConfig().entities.spawning;
        var mobs = new java.util.ArrayList<net.minecraft.world.entity.Mob>();
        for (var type : List.of(net.minecraft.world.entity.EntityTypes.SQUID, net.minecraft.world.entity.EntityTypes.COD,
                net.minecraft.world.entity.EntityTypes.ZOMBIE)) {
            var mob = type.create(world, EntitySpawnReason.NATURAL);
            check(mob != null, "Could not create spawn accounting mob");
            mob.setPos(marker.getX() + 0.5, 18, marker.getZ() + 0.5);
            ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$pushAddEntityReason(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL);
            check(world.addFreshEntity(mob), "Could not add spawn accounting mob");
            mobs.add(mob);
        }
        var expected = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<net.minecraft.world.entity.MobCategory>();
        for (var entity : world.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.Mob mob && (mob.isPersistenceRequired() || mob.requiresCustomPersistence())) continue;
            var category = entity.getType().getCategory();
            if (category != net.minecraft.world.entity.MobCategory.MISC
                && gr1mly4memes.felt.mod.server.world.FeltMobSpawning.countsForSpawning(world, entity)
                && world.getChunkSource().getChunkNow(entity.chunkPosition().x(), entity.chunkPosition().z()) != null) expected.addTo(category, 1);
        }
        world.getChunkSource().tick(() -> false, true);
        var actual = world.getChunkSource().getLastSpawnState().getMobCategoryCounts();
        for (var entry : expected.object2IntEntrySet()) {
            check(actual.getInt(entry.getKey()) == entry.getIntValue(),
                "Natural spawning ignored loaded mobs: " + entry.getKey() + " expected " + entry.getIntValue() + " but counted " + actual.getInt(entry.getKey()));
        }
        var customFish = net.minecraft.world.entity.EntityTypes.COD.create(world, EntitySpawnReason.COMMAND);
        customFish.setPos(marker.getX() + 0.5, 18, marker.getZ() + 0.5);
        ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$pushAddEntityReason(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
        check(world.addFreshEntity(customFish), "Could not add custom fish");
        world.getChunkSource().tick(() -> false, true);
        check(world.getChunkSource().getLastSpawnState().getMobCategoryCounts().getInt(net.minecraft.world.entity.MobCategory.WATER_AMBIENT) == expected.getInt(net.minecraft.world.entity.MobCategory.WATER_AMBIENT), "Custom fish incorrectly consumed the natural cap");
        spawning.countAllMobsForSpawning = true;
        world.getChunkSource().tick(() -> false, true);
        check(world.getChunkSource().getLastSpawnState().getMobCategoryCounts().getInt(net.minecraft.world.entity.MobCategory.WATER_AMBIENT) == expected.getInt(net.minecraft.world.entity.MobCategory.WATER_AMBIENT) + 1, "count-all-mobs-for-spawning did not include custom fish");
        spawning.countAllMobsForSpawning = false;
        customFish.discard();
        verifyPlayerMobCaps(world, marker, mobs.get(1));
        var spawnState = world.getChunkSource().getLastSpawnState();
        int waterBefore = spawnState.getMobCategoryCounts().getInt(net.minecraft.world.entity.MobCategory.WATER_AMBIENT);
        ((gr1mly4memes.felt.bridge.core.world.spawner.WorldEntitySpawnerBridge.EntityDensityManagerBridge)spawnState)
            .bridge$updateDensity(mobs.get(1), world.getChunkSource().getChunkNow(marker.getX() >> 4, marker.getZ() >> 4));
        check(spawnState.getMobCategoryCounts().getInt(net.minecraft.world.entity.MobCategory.WATER_AMBIENT) == waterBefore + 1,
            "Per-player spawn completion did not update density without a vanilla local cap calculator");
        for (var mob : mobs) mob.discard();
        world.getChunkSource().tick(() -> false, true);
        var afterRemoval = world.getChunkSource().getLastSpawnState().getMobCategoryCounts();
        for (var mob : mobs) check(afterRemoval.getInt(mob.getType().getCategory()) == expected.getInt(mob.getType().getCategory()) - 1,
            "Removed mob remained in spawn accounting");
        System.out.println("Native natural spawning counts loaded land, water and hostile mobs, and tracks removals");
    }

    private static void verifyPlayerMobCaps(ServerLevel world, BlockPos marker, net.minecraft.world.entity.Mob fish) {
        var nearby = ((ChunkSystemServerLevel)world).moonrise$getNearbyPlayers();
        var first = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "CapFirst"), net.minecraft.server.level.ClientInformation.createDefault());
        var second = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "CapSecond"), net.minecraft.server.level.ClientInformation.createDefault());
        var far = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "CapDistant"), net.minecraft.server.level.ClientInformation.createDefault());
        first.setPos(marker.getX(), 18, marker.getZ());
        second.setPos(marker.getX() + 1, 18, marker.getZ());
        far.setPos(marker.getX() + 1600, 18, marker.getZ());
        nearby.addPlayer(first); nearby.addPlayer(second); nearby.addPlayer(far);
        try {
            var category = net.minecraft.world.entity.MobCategory.WATER_AMBIENT;
            var spawning = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$paperConfig().entities.spawning;
            gr1mly4memes.felt.mod.server.world.FeltMobSpawning.track(world, fish);
            check(gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(first, category) == 1 && gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(second, category) == 1,
                "A shared mob did not count against both nearby players");
            check(gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(far, category) == 0, "A distant player consumed another player's mob cap");
            int limit = ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge)world).bridge$getWorld().getSpawnLimit(org.bukkit.entity.SpawnCategory.WATER_AMBIENT);
            check(gr1mly4memes.felt.mod.server.world.FeltMobSpawning.remaining(world, fish.chunkPosition(), category) == limit - 1, "Per-player remaining budget is incorrect");
            gr1mly4memes.felt.mod.server.world.FeltMobSpawning.failure(world, fish.chunkPosition(), category);
            check(gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(first, category) == 2, "Cancelled spawning did not apply player backoff");
            gr1mly4memes.felt.mod.server.world.FeltMobSpawning.reset(first);
            check(gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(first, category) == 0, "Tick reset did not reset counts and decay cancellation backoff");
            for (int i = 0; i < limit; ++i) gr1mly4memes.felt.mod.server.world.FeltMobSpawning.track(world, fish);
            check(gr1mly4memes.felt.mod.server.world.FeltMobSpawning.remaining(world, fish.chunkPosition(), category) <= 0, "A full player cap still allowed spawning");
            verifyCancelledSpawnEvents(world, marker, first, second);
            spawning.perPlayerMobSpawns = false;
            int before = gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(first, category);
            gr1mly4memes.felt.mod.server.world.FeltMobSpawning.track(world, fish);
            check(gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(first, category) == before, "Disabled per-player caps still changed player counts");
            spawning.perPlayerMobSpawns = true;
        } finally {
            nearby.removePlayer(first); nearby.removePlayer(second); nearby.removePlayer(far);
        }
        System.out.println("Paper per-player caps: overlap, isolation, limits, cancellation backoff and configuration passed");
    }

    private static void verifyCancelledSpawnEvents(ServerLevel world, BlockPos marker,
            net.minecraft.server.level.ServerPlayer player, net.minecraft.server.level.ServerPlayer nearby) {
        var plugin = verificationPlugin();
        var listener = new org.bukkit.event.Listener() {};
        var aborted = new java.util.concurrent.atomic.AtomicBoolean();
        var events = new java.util.concurrent.atomic.AtomicInteger();
        var registered = new org.bukkit.plugin.RegisteredListener(listener, (ignored, event) -> {
            var spawn = (com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent)event;
            events.incrementAndGet();
            spawn.setCancelled(true);
            spawn.setShouldAbortSpawn(aborted.get());
        }, org.bukkit.event.EventPriority.NORMAL, plugin, false);
        var handlers = com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent.getHandlerList();
        handlers.register(registered);
        var oldPos = player.position();
        player.setPos(marker.getX() - 32, world.getMaxY() - 20, marker.getZ());
        world.players().add(player);
        try {
            int before = gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(nearby, net.minecraft.world.entity.MobCategory.MONSTER);
            var chunk = world.getChunkSource().getChunkNow(marker.getX() >> 4, marker.getZ() >> 4);
            var start = new BlockPos(marker.getX(), world.getMaxY() - 20, marker.getZ());
            for (int attempt = 0; attempt < 8 && events.get() == 0; ++attempt)
                net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(net.minecraft.world.entity.MobCategory.MONSTER, world, chunk, start,
                    (type, level, pos, access) -> false, (mob, access) -> { throw new AssertionError("Cancelled pre-spawn event still added a mob"); });
            check(events.get() > 0 && gr1mly4memes.felt.mod.server.world.FeltMobSpawning.count(nearby, net.minecraft.world.entity.MobCategory.MONSTER) > before,
                "A real cancelled PreCreatureSpawnEvent did not apply nearby-player backoff");
            aborted.set(true);
            int eventsBefore = events.get();
            for (int attempt = 0; attempt < 8 && events.get() == eventsBefore; ++attempt)
                net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(net.minecraft.world.entity.MobCategory.MONSTER, world, chunk, start,
                    (type, level, pos, access) -> false, (mob, access) -> { throw new AssertionError("Aborted pre-spawn event still added a mob"); });
            check(events.get() == eventsBefore + 1, "An aborting pre-spawn event did not stop the native spawn loop");
        } finally {
            handlers.unregister(registered);
            world.players().remove(player);
            player.setPos(oldPos.x, oldPos.y, oldPos.z);
        }
    }

    private static org.bukkit.plugin.Plugin verificationPlugin() {
        return verificationPlugin("FeltParityVerification", () -> true);
    }

    private static org.bukkit.plugin.Plugin verificationPlugin(String name, java.util.function.BooleanSupplier enabled) {
        var description = new org.bukkit.plugin.PluginDescriptionFile(name, "1.0", MoonriseWorldTest.class.getName());
        return (org.bukkit.plugin.Plugin)java.lang.reflect.Proxy.newProxyInstance(org.bukkit.plugin.Plugin.class.getClassLoader(),
            new Class<?>[]{org.bukkit.plugin.Plugin.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "isEnabled" -> enabled.getAsBoolean();
                case "getName", "toString" -> name;
                case "getPluginMeta", "getDescription" -> description;
                case "getLogger" -> java.util.logging.Logger.getLogger(name);
                case "getServer" -> org.bukkit.Bukkit.getServer();
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> null;
            });
    }

    private static void verifyDimension(ServerLevel world) {
        var scheduler = ((ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler();
        CompletableFuture<ChunkAccess> loaded = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> scheduler.scheduleChunkLoad(12, -12, ChunkStatus.FULL, true, Priority.HIGH, chunk -> {
            check(TickThread.isTickThread(), "Asynchronous load callback did not return to the tick thread");
            loaded.complete(chunk);
        }), Util.backgroundExecutor()).exceptionally(failure -> { loaded.completeExceptionally(failure); return null; });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        world.getChunkSource().mainThreadProcessor.managedBlock(() -> {
            check(System.nanoTime() < deadline, "Asynchronous dimension generation timed out: " + world.dimension().identifier());
            return loaded.isDone();
        });
        check(loaded.join() instanceof LevelChunk && loaded.join().isLightCorrect(), "Dimension did not finish generation and lighting");
        if (world.dimension() == net.minecraft.world.level.Level.END) {
            PaperGameplayApiTest.verifyDragon(world, new BlockPos(12 * 16 + 8, 100, -12 * 16 + 8));
        }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
