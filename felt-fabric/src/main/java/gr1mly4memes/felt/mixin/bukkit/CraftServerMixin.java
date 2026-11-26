package gr1mly4memes.felt.mixin.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import gr1mly4memes.felt.bridge.bukkit.CraftServerBridge;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.server.dedicated.DedicatedServerBridge;
import gr1mly4memes.felt.bridge.core.world.level.storage.LevelStorageSourceBridge;
import gr1mly4memes.felt.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import gr1mly4memes.felt.mod.server.world.BukkitWorldIdentity;
import gr1mly4memes.felt.mod.server.world.WorldCreationContext;
import net.kyori.adventure.key.Key;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import gr1mly4memes.felt.mod.config.FeltConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.validation.ContentValidationException;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.generator.CraftWorldInfo;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.SimplePluginManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@Mixin(value = CraftServer.class, remap = false)
public abstract class CraftServerMixin implements CraftServerBridge {

    // @formatter:off
    @Shadow @Final private SimplePluginManager pluginManager;
    @Shadow @Final protected DedicatedServer console;
    @Shadow @Final @Mutable protected DedicatedPlayerList playerList;
    @Shadow @Final @Mutable private List<CraftPlayer> playerView;
    @Shadow @Final private Map<String, World> worlds;
    @Accessor("logger") @Mutable public abstract void setLogger(Logger logger);
    @Shadow public abstract ChunkGenerator getGenerator(String world);
    @Shadow public abstract BiomeProvider getBiomeProvider(String world);
    @Shadow public abstract GameMode getDefaultGameMode();
    // @formatter:on

    @Shadow
    public abstract File getWorldContainer();

    @Shadow
    public abstract World getWorld(String name);

    @Shadow
    public abstract World getWorld(UUID uid);

    @Shadow
    public abstract World getWorld(Key worldKey);

    @Shadow
    public abstract DedicatedServer getServer();

    @Overwrite
    public String getVersion() {
        return System.getProperty("felt.version") + " (MC: " + this.console.getServerVersion() + ")";
    }

    @Override
    public void bridge$setPlayerList(PlayerList playerList) {
        // Some plugin may change to a different PlayerList
        this.playerList = (DedicatedPlayerList) playerList;
        this.playerView = Collections.unmodifiableList(Lists.transform(playerList.players, player ->
                ((ServerPlayerBridge)player).bridge$getBukkitEntity()
                ));
    }

    @Override
    public void bridge$removeWorld(ServerLevel world) {
        if (world == null) {
            return;
        }
        this.worlds.remove(world.bridge$getWorld().getName().toLowerCase(Locale.ROOT), world.bridge$getWorld());
    }

    @Inject(method = "unloadWorld(Lorg/bukkit/World;Z)Z", at = @At("RETURN"), remap = false)
    private void felt$discardUnloadedWorld(World world, boolean save, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && world instanceof CraftWorld) {
            WorldCreationContext.discard(((CraftWorld) world).getHandle());
        }
    }

    private final Map<String, ChunkGenerator> generatorCache = new HashMap<>();
    private final Map<String, BiomeProvider> biomeProviderCache = new HashMap<>();
    private final Map<String, World.Environment> environmentCache = new HashMap<>();

    @Override
    public void bridge$offerGeneratorCache(String name, ChunkGenerator generator) {
        // Newly created level
        generatorCache.put(name, generator);
    }

    @Override
    public ChunkGenerator bridge$consumeGeneratorCache(String name) {
        var cache = generatorCache.remove(name);
        if (cache == null) {
            // If not provided (which means it's not newly created),
            // load from bukkit.yml configuration.
            // See CraftServer
            cache = getGenerator(name);
        }
        return cache;
    }

    @Override
    public BiomeProvider bridge$consumeBiomeProviderCache(String name) {
        var cache = biomeProviderCache.remove(name);
        if (cache == null) {
            // If not provided (which means it's not newly created),
            // load from bukkit.yml configuration.
            // See CraftServer
            cache = getBiomeProvider(name);
        }
        return cache;
    }

    @Override
    public void bridge$offerBiomeProviderCache(String name, BiomeProvider provider) {
        // Newly created level
        biomeProviderCache.put(name, provider);
    }

    @Override
    public World.Environment bridge$consumeEnvironmentCache(String name) {
        return environmentCache.remove(name);
    }

    @Override
    public void bridge$offerEnvironmentCache(String name, World.Environment environment) {
        environmentCache.put(name, environment);
    }

    /**
     * @author InitAuther97
     * @reason experimental base generator setting & support for CUSTOM environment
     */
    @Override
    public World bridge$createWorld(WorldCreator creator) {
        Preconditions.checkState(this.console.getAllLevels().iterator().hasNext(), "Cannot create additional worlds on STARTUP");
        Preconditions.checkArgument(creator != null, "WorldCreator cannot be null");
        String name = creator.name();
        ChunkGenerator generator = creator.generator();
        BiomeProvider biomeProvider = creator.biomeProvider();
        File folder = new File(this.getWorldContainer(), name);
        World world = this.getWorld(name);
        World worldByKey = this.getWorld(creator.key());
        if (world != null || worldByKey != null) {
            if (world == worldByKey) {
                return world;
            }
            throw new IllegalArgumentException("Cannot create a world with key " + creator.key() + " and name " + name + " one (or both) already match a world that exists");
        } else {
            if (folder.exists()) {
                Preconditions.checkArgument(folder.isDirectory(), "File (%s) exists and isn't a folder", name);
            }

            if (generator == null) {
                generator = this.getGenerator(name);
            }

            if (biomeProvider == null) {
                biomeProvider = this.getBiomeProvider(name);
            }

            // Felt start: handle CUSTOM
            ResourceKey<LevelStem> actualDimension;
            boolean isCustom = false;
            switch (creator.environment()) {
                case NORMAL -> actualDimension = LevelStem.OVERWORLD;
                case NETHER -> actualDimension = LevelStem.NETHER;
                case THE_END -> actualDimension = LevelStem.END;
                case CUSTOM -> {
                    if (FeltConfig.replaceableBaseWorldGenerator) {
                        isCustom = true;
                        final var location = Identifier.tryBuild("bukkit", name);
                        if (location == null) {
                            throw new IllegalArgumentException("Illegal world name: " + name);
                        }
                        actualDimension = ResourceKey.create(Registries.LEVEL_STEM, location);
                    } else {
                        throw new IllegalArgumentException("Illegal dimension (" + creator.environment() + ")");
                    }
                }
                default -> throw new IllegalArgumentException("Illegal dimension (" + creator.environment() + ")");
            }
            // Felt end

            LevelStorageSource.LevelStorageAccess worldSession;
            try {
                worldSession = ((LevelStorageSourceBridge) LevelStorageSource.createDefault(this.getWorldContainer().toPath())).felt$validateAndCreateAccess(name, actualDimension);
            } catch (ContentValidationException | IOException ex) {
                throw new RuntimeException(ex);
            }

            boolean added = false;
            Throwable failure = null;
            // Session ownership boundary: retain storage for a successfully published world.
            try {
                WorldLoader.DataLoadContext context = ((DedicatedServerBridge) this.console).felt$dataLoadContext();
                Dynamic<?> dynamic;
                if (worldSession.hasWorldData()) {
                    try {
                        dynamic = worldSession.getUnfixedDataTagWithFallback();
                    } catch (ReportedNbtException | IOException | NbtException ioexception) {
                        MinecraftServer.LOGGER.error("Failed to load world data. World files may be corrupted. Shutting down.", ioexception);
                        return null;
                    }
                    LevelSummary worldinfo = worldSession.fixAndGetSummaryFromTag(dynamic);
                    if (worldinfo.requiresManualConversion()) {
                        MinecraftServer.LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
                        return null;
                    }
                    if (!worldinfo.isCompatible()) {
                        MinecraftServer.LOGGER.info("This world was created by an incompatible version.");
                        return null;
                    }
                    dynamic = DataFixers.getFileFixer().fix(worldSession, dynamic, new UpgradeProgress());
                    // FileFixer at version 4772 migrates legacy level.dat WorldGenSettings
                    // into data/minecraft/world_gen_settings.dat before this check. Missing after
                    // that upgrade means recovery is not proven safe: never accept random fallback.
                    var savedWorldgen = LevelStorageSource.readExistingSavedData(worldSession, context.datapackWorldRegistries(), WorldGenSettings.TYPE);
                    if (savedWorldgen.result().isEmpty()) {
                        throw new IllegalStateException("Missing or invalid world generation settings for " + name
                            + " (data/minecraft/world_gen_settings.dat); refusing to substitute a random seed: " + savedWorldgen.error());
                    }
                } else {
                    dynamic = null;
                }

                boolean hardcore = creator.hardcore();
                RegistryAccess.Frozen datapackDimensions = context.datapackDimensions();
                Registry<LevelStem> datapackStems = datapackDimensions.lookupOrThrow(Registries.LEVEL_STEM);
                RegistryAccess.Frozen dimensions;
                PrimaryLevelData levelData;
                LevelDataAndDimensions.WorldDataAndGenSettings dataAndSettings;
                if (dynamic != null) {
                    LevelDataAndDimensions levelDataAndDimensions = LevelStorageSource.getLevelDataAndDimensions(worldSession, dynamic, context.dataConfiguration(), datapackStems, context.datapackWorldRegistries());
                    dataAndSettings = levelDataAndDimensions.worldDataAndGenSettings();
                    levelData = (PrimaryLevelData) dataAndSettings.data();
                    // For CUSTOM, the server's experimental stem registry supplies the stem;
                    // loaded generation settings/options still belong to this world's files.
                    dimensions = isCustom ? this.console.registries().getLayer(RegistryLayer.DIMENSIONS)
                        : levelDataAndDimensions.dimensions().dimensionsRegistryAccess();
                } else {
                    WorldOptions options = new WorldOptions(creator.seed(), creator.generateStructures(), creator.bonusChest());
                    LevelSettings settings = new LevelSettings(name, GameType.byId(this.getDefaultGameMode().getValue()), new LevelSettings.DifficultySettings(Difficulty.EASY, hardcore, false), false, context.dataConfiguration());
                    var registries = context.datapackWorldRegistries();
                    String generatorSettings = creator.generatorSettings();
                    if (generatorSettings.isEmpty() && creator.type() == WorldType.FLAT) {
                        var defaults = FlatLevelGeneratorSettings.getDefault(registries.lookupOrThrow(Registries.BIOME),
                            registries.lookupOrThrow(Registries.STRUCTURE_SET), registries.lookupOrThrow(Registries.PLACED_FEATURE));
                        generatorSettings = FlatLevelGeneratorSettings.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), defaults)
                            .getOrThrow().toString();
                    }
                    var properties = new DedicatedServerProperties.WorldDimensionData(
                        GsonHelper.parse(generatorSettings.isEmpty() ? "{}" : generatorSettings), creator.type().name().toLowerCase(Locale.ROOT));
                    WorldDimensions worldDimensions = properties.create(registries);
                    WorldDimensions.Complete baked = worldDimensions.bake(datapackStems);
                    Lifecycle lifecycle = baked.lifecycle().add(registries.allRegistriesLifecycle());
                    levelData = new PrimaryLevelData(settings, baked.specialWorldProperty(), lifecycle);
                    dimensions = baked.dimensionsRegistryAccess();
                    dataAndSettings = new LevelDataAndDimensions.WorldDataAndGenSettings(levelData, new WorldGenSettings(options, worldDimensions));
                }
                WorldOptions options = dataAndSettings.genSettings().options();

                Registry<LevelStem> stems = dimensions.lookupOrThrow(Registries.LEVEL_STEM);
                LevelStem stem = dataAndSettings.genSettings().dimensions().get(actualDimension).orElseGet(() -> stems.getValue(actualDimension));
                if (stem == null) {
                    throw new IllegalArgumentException("Unknown level stem: " + actualDimension);
                }
                if (actualDimension != null) {
                    ((PrimaryLevelDataBridge) levelData).felt$offerCustomDimensions(stems);
                }
                ((PrimaryLevelDataBridge) levelData).felt$checkName(name);
                levelData.setModdedInfo(this.console.getServerModName(), this.console.getModdedStatus().shouldReportAsModified());

                ((DedicatedServerBridge) this.console).felt$forceUpgradeIfNeeded(worldSession, levelData, dimensions); // Felt

                // WorldGenSettings.TYPE uses data/minecraft/world_gen_settings.dat.
                // Persist before publishing the world: otherwise its seed silently randomizes on reopen.
                if (dynamic == null) {
                    try {
                        worldSession.saveDataTag(levelData);
                        RegistryAccess.Frozen worldgenForSave = new RegistryAccess.ImmutableRegistryAccess(
                            java.util.stream.Stream.concat(
                                this.console.registries().getAccessForLoading(RegistryLayer.DIMENSIONS).registries(),
                                dimensions.registries()
                            )
                        ).freeze();
                        LevelStorageSource.writeWorldGenSettings(worldgenForSave, worldSession.getLevelPath(LevelResource.ROOT), dataAndSettings.genSettings());
                    } catch (IOException ex) {
                        throw new RuntimeException("Unable to persist world generation settings for " + name, ex);
                    }
                }
                // Reopened saves own their generation options; a new creator seed must not
                // change the biome source for already-generated neighboring chunks.
                long j = BiomeManager.obfuscateSeed(options.seed());
                List<CustomSpawner> list = new ArrayList<>(ImmutableList.of(new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege()));
                ResourceKey<net.minecraft.world.level.Level> worldKey = org.bukkit.craftbukkit.util.CraftNamespacedKey.toResourceKey(Registries.DIMENSION, creator.key());
                UUID worldUid = BukkitWorldIdentity.getUUID(worldSession, worldKey);
                DimensionType dimensionType = stem.type().value();

                WorldInfo worldInfo = new CraftWorldInfo(levelData.getLevelName(), org.bukkit.craftbukkit.util.CraftNamespacedKey.fromMinecraft(worldKey.identifier()), options.seed(), levelData.enabledFeatures(), creator.environment(), dimensionType, stem.generator(), this.console.registryAccess(), worldUid);
                if (biomeProvider == null && generator != null) {
                    biomeProvider = generator.getDefaultBiomeProvider(worldInfo);
                }

                // Duplicate UID is rejected by CraftServer.addWorld during the constructor.
                // Reject before construction so no unowned level/storage is allocated.
                World duplicate = this.getWorld(worldUid);
                if (duplicate != null) {
                    MinecraftServer.LOGGER.warn("Refusing to create world {}: UUID {} is already used by {}", name, worldUid, duplicate.getName());
                    return null;
                }

                this.bridge$offerBiomeProviderCache(name, biomeProvider);
                this.bridge$offerGeneratorCache(name, generator);
                this.bridge$offerEnvironmentCache(name, creator.environment());
                ServerLevel internal = null;
                WorldCreationContext.begin(dataAndSettings.genSettings());
                try {
                    internal = new ServerLevel(this.console, this.console.executor, worldSession, levelData, worldKey, stem, levelData.isDebugWorld(), j, (List)(creator.environment() == World.Environment.NORMAL ? list : ImmutableList.of()), true);
                    WorldCreationContext.attach(internal);
                    if (this.worlds.get(name.toLowerCase(Locale.ROOT)) != internal.bridge$getWorld()) {
                        return null; // duplicate UID/name was rejected in constructor; frame cleanup discards it
                    }
                    ((DedicatedServerBridge) this.console).felt$prepareAndAddLevel(internal, levelData, creator);
                    CraftWorld bukkit = internal.bridge$getWorld();
                    this.pluginManager.callEvent(new WorldLoadEvent(bukkit));
                    WorldCreationContext.complete(internal);
                    added = true;
                    return bukkit;
                } catch (RuntimeException | Error ex) {
                    failure = ex;
                    throw ex;
                } finally {
                    Throwable cleanupFailure = null;
                    try {
                        if (!added) {
                            if (internal != null) {
                                try {
                                    // addLevel publishes before notifying loaders. The callback itself
                                    // may throw, or a nested creation may replace the dimension.
                                    ((gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge) this.console)
                                        .felt$rollbackPublishedLevel(internal);
                                } catch (RuntimeException | Error ex) {
                                    cleanupFailure = ex;
                                }
                            }
                            // addWorld can publish during ServerLevel construction, before it returns.
                            // Remove a world published before a failed constructor returned.
                            World registered = this.worlds.get(name.toLowerCase(Locale.ROOT));
                            if (registered instanceof CraftWorld && WorldCreationContext.ownsPending(((CraftWorld) registered).getHandle())
                                && (internal == null || ((CraftWorld) registered).getHandle() == internal)) {
                                this.worlds.remove(name.toLowerCase(Locale.ROOT), registered);
                            }
                            if (internal != null) {
                                WorldCreationContext.discard(internal);
                            }
                            this.biomeProviderCache.remove(name);
                            this.generatorCache.remove(name);
                            this.environmentCache.remove(name);
                        }
                    } catch (RuntimeException | Error ex) {
                        if (cleanupFailure == null) cleanupFailure = ex;
                        else if (cleanupFailure != ex) cleanupFailure.addSuppressed(ex);
                    } finally {
                        try {
                            WorldCreationContext.clearPending();
                        } catch (RuntimeException | Error ex) {
                            if (cleanupFailure == null) cleanupFailure = ex;
                            else if (cleanupFailure != ex) cleanupFailure.addSuppressed(ex);
                        }
                    }
                    if (cleanupFailure != null) {
                        if (failure != null) {
                            if (failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
                        } else if (cleanupFailure instanceof Error error) {
                            throw error;
                        } else {
                            throw (RuntimeException) cleanupFailure;
                        }
                    }
                }
            } catch (RuntimeException | Error ex) {
                failure = ex;
                throw ex;
            } finally {
                if (!added) {
                    try {
                        worldSession.close();
                    } catch (IOException ex) {
                        if (failure != null) {
                            if (failure != ex) failure.addSuppressed(ex);
                        } else {
                            MinecraftServer.LOGGER.warn("Failed to close world storage for {}", name, ex);
                        }
                    } catch (RuntimeException | Error ex) {
                        if (failure != null) {
                            if (failure != ex) failure.addSuppressed(ex);
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }
}
