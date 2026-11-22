package gr1mly4memes.felt.mixin.core.server.level;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import gr1mly4memes.felt.bridge.bukkit.CraftServerBridge;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.world.IInventoryBridge;
import gr1mly4memes.felt.bridge.core.world.level.border.WorldBorderBridge;
import gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge;
import gr1mly4memes.felt.bridge.core.world.level.ExplosionBridge;
import gr1mly4memes.felt.bridge.core.world.level.levelgen.flat.FlatLevelGeneratorSettingsBridge;
import gr1mly4memes.felt.bridge.core.world.server.ServerChunkProviderBridge;
import gr1mly4memes.felt.bridge.core.server.level.ServerLevelBridge;
import gr1mly4memes.felt.bridge.core.world.level.storage.DerivedLevelDataBridge;
import gr1mly4memes.felt.bridge.core.world.level.storage.LevelStorageSourceBridge;
import gr1mly4memes.felt.bridge.core.world.level.saveddata.maps.MapItemSavedDataBridge;
import gr1mly4memes.felt.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import gr1mly4memes.felt.mixin.core.world.level.LevelMixin;
import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import gr1mly4memes.felt.mod.server.FeltServer;
import gr1mly4memes.felt.mod.server.entity.FeltSpawnReason;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felt.mod.server.world.BukkitWorldIdentity;
import gr1mly4memes.felt.mod.server.world.LevelPersistentData;
import gr1mly4memes.felt.mod.server.world.WorldSymlink;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felt.mod.util.DelegateWorldInfo;
import gr1mly4memes.felt.mod.util.DistValidate;
import gr1mly4memes.felt.mod.config.FeltConfig;
import gr1mly4memes.felt.mod.server.world.WorldCreationContext;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.generator.CustomChunkGenerator;
import org.bukkit.craftbukkit.generator.CustomWorldChunkManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends LevelMixin implements ServerLevelBridge {

    // @formatter:off
    @Shadow public abstract boolean addFreshEntity(Entity entityIn);
    @Shadow public abstract boolean addWithUUID(Entity entityIn);
    @Shadow public abstract <T extends ParticleOptions> int sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed);
    @Shadow protected abstract boolean sendParticles(ServerPlayer player, boolean longDistance, double posX, double posY, double posZ, Packet<?> packet);
    @Shadow @Nonnull public abstract MinecraftServer getServer();
    @Shadow @Final private List<ServerPlayer> players;
    @Shadow public abstract ServerChunkCache getChunkSource();
    @Shadow protected abstract void wakeUpAllPlayers();
    @Shadow @Final private ServerChunkCache chunkSource;
    @Shadow @Final public ServerLevelData serverLevelData;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;
    @Shadow public abstract SavedDataStorage getDataStorage();
    @Shadow public abstract long getSeed();
    @Shadow public abstract GameRules getGameRules();
    @Shadow protected abstract void addPlayer(ServerPlayer serverPlayer);
    @Shadow @Nullable public abstract Entity getEntity(int i);
    @Shadow public abstract void sendBlockUpdated(BlockPos blockPos, BlockState blockState, BlockState blockState2, int i);
    @Shadow @javax.annotation.Nullable private EnderDragonFight dragonFight;
    // @formatter:on

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    public PrimaryLevelData K; // Stupid CraftBukkit patch.

    @Override
    public PrimaryLevelData bridge$getPrimaryLevelData() {
        return K;
    }
    public LevelStorageSource.LevelStorageAccess storageSource; // Paper added
    public UUID uuid;
    public ResourceKey<LevelStem> typeKey;

    @Override
    public ResourceKey<LevelStem> getTypeKey() {
        return this.typeKey;
    }

    @ShadowConstructor
    public void felt$constructor(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, ServerLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void felt$constructor(MinecraftServer server, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, PrimaryLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider) {
        var craftBridge = (CraftServerBridge)(Object) ((MinecraftServerBridge) server).bridge$getServer();
        assert craftBridge != null; // Already checked in bridge
        // We have no way but store it somewhere and use a default value
        // in order to avoid having to pass them as arguments.
        craftBridge.bridge$offerEnvironmentCache(worldInfo.getLevelName(), env);
        craftBridge.bridge$offerGeneratorCache(worldInfo.getLevelName(), gen);
        craftBridge.bridge$offerBiomeProviderCache(worldInfo.getLevelName(), biomeProvider);
        felt$constructor(server, backgroundExecutor, levelSave, worldInfo, dimension, levelStem, isDebug, seed, specialSpawners, shouldBeTicking);
    }

    // Support custom chunk generator; in consistency with CraftBukkit
    // The real part is inside ServerChunkCache, when initializing ChunkMap (in ctor).
    // A generator state is created, which is later used for chunk generation.
    // Previously we didn't modify it before ChunkMap is created,
    // which in turn cause custom world generation from Bukkit failing to work.
    @Decorate(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/LevelStem;generator()Lnet/minecraft/world/level/chunk/ChunkGenerator;"))
    private ChunkGenerator felt$initChunkGenerator(LevelStem instance, @Local(ordinal = -1) MinecraftServer server, @Local(ordinal = -1) ServerLevelData worldInfo) throws Throwable {
        WorldCreationContext.attachIfPresent((ServerLevel) (Object) this);
        // Pulling up world info init since level info is used when selecting ChunkGenerator.
        if (felt$isActual() && worldInfo instanceof PrimaryLevelData primary) {
            this.K = primary;
        } else {
            // damn spigot again
            this.K = DelegateWorldInfo.wrap(worldInfo);
        }

        if (felt$isActual()) {
            var craftBridge = (CraftServerBridge) (Object) ((MinecraftServerBridge) server).bridge$getServer();

            this.biomeProvider = craftBridge.bridge$consumeBiomeProviderCache(worldInfo.getLevelName());
            this.generator = craftBridge.bridge$consumeGeneratorCache(worldInfo.getLevelName());
            this.environment = craftBridge.bridge$consumeEnvironmentCache(worldInfo.getLevelName());

            if (this.environment == null) {
                // Select world environment for vanilla/mod world creation
                if (instance.type().is(LevelStem.OVERWORLD.identifier())) {
                    this.environment = World.Environment.NORMAL;
                } else if (instance.type().is(LevelStem.NETHER.identifier())) {
                    this.environment = World.Environment.NETHER;
                } else if (instance.type().is(LevelStem.END.identifier())) {
                    this.environment = World.Environment.THE_END;
                } else {
                    // Don't use CUSTOM; it's not even supported in Multiverse
                    // this.environment = World.Environment.CUSTOM;
                    this.environment = World.Environment.NORMAL;
                }
            }

            // Now we create the CraftWorld
            this.world = new CraftWorld((ServerLevel) (Object) this, org.bukkit.craftbukkit.util.CraftNamespacedKey.fromMinecraft(this.dimension().identifier()), biomeProvider, environment);
        }

        ChunkGenerator raw = (ChunkGenerator) DecorationOps.callsite().invoke(instance);
        if (felt$isActual()) {
            // Data needed by getWorld() are all initialized for possible creating CraftWorld.
            // CraftBukkit start: select custom chunk generator
            if (biomeProvider != null) {
                BiomeSource biomeSource = new CustomWorldChunkManager(getWorld(), biomeProvider, raw.getBiomeSource());
                if (raw instanceof NoiseBasedChunkGenerator noise) {
                    raw = new NoiseBasedChunkGenerator(biomeSource, noise.settings);
                } else if (raw instanceof FlatLevelSource flat) {
                    raw = new FlatLevelSource(((FlatLevelGeneratorSettingsBridge) flat.settings()).bridge$withBiomeSource(biomeSource));
                } else {
                    FeltServer.LOGGER.warn("Level {} has unknown customized generator -- requested biome provider won't be satisfied.", this.serverLevelData.getLevelName());
                }
            }
            if (generator != null) {
                raw = new CustomChunkGenerator((ServerLevel) (Object) this, raw, generator);
            }
            // CraftBukkit end
        }
        return raw;
    }

    @Decorate(method = "<init>(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/world/level/storage/ServerLevelData;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/dimension/LevelStem;ZJLjava/util/List;Z)V",
        inject = true, at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;chunkSource:Lnet/minecraft/server/level/ServerChunkCache;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER),
        require = 1, expect = 1, allow = 1)
    private void felt$initWanderingTraderSpawner(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, ServerLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, boolean isDebug, long seed, List<CustomSpawner> spawners, boolean shouldBeTicking) {
        if (WorldCreationContext.ownsPending((ServerLevel) (Object) this) && this.environment == World.Environment.NORMAL) {
            // The chunk source now owns the storage; populate the list before mod loader copies it for its event.
            spawners.add(new net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner(this.getDataStorage()));
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$init(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, ServerLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, CallbackInfo ci) {
        this.pvpMode = this.getGameRules().get(GameRules.PVP);
        for (var category : org.bukkit.entity.SpawnCategory.values()) {
            if (org.bukkit.craftbukkit.util.CraftSpawnCategory.isValidForLimits(category)) {
                this.ticksPerSpawnCategory.put(category, this.getCraftServer().getTicksPerSpawns(category));
            }
        }
        this.storageSource = levelSave;
        var worldGenSettings = WorldCreationContext.lookup((ServerLevel) (Object) this);
        if (worldGenSettings != null) {
            this.getDataStorage().set(WorldGenSettings.TYPE, worldGenSettings);
        }
        if (felt$isActual() && this.dragonFight == null && this.environment == World.Environment.THE_END) {
            this.dragonFight = this.getDataStorage().computeIfAbsent(EnderDragonFight.TYPE);
            this.dragonFight.init((ServerLevel) (Object) this, this.getSeed(), BlockPos.ZERO);
        }
        var typeKey = ((LevelStorageSourceBridge.LevelStorageAccessBridge) levelSave).bridge$getTypeKey();
        if (typeKey != null) {
            this.typeKey = typeKey;
        } else {
            var dimensions = getServer().registryAccess().lookupOrThrow(Registries.LEVEL_STEM);
            var key = dimensions.getResourceKey(levelStem);
            if (key.isPresent()) {
                this.typeKey = key.get();
            } else {
                FeltServer.LOGGER.warn("Assign {} to unknown level stem {}", dimension.identifier(), levelStem);
                this.typeKey = ResourceKey.create(Registries.LEVEL_STEM, dimension.identifier());
            }
            if (worldInfo instanceof DerivedLevelData data) {
                ((DerivedLevelDataBridge) worldInfo).bridge$setDimType(this.getTypeKey());
                if (FeltConfig.symlinkWorld) {
                    WorldSymlink.create(data, levelSave.getDimensionPath(this.dimension()).toFile());
                }
            }
        }
        this.spigotConfig = new SpigotWorldConfig(worldInfo.getLevelName(), org.bukkit.craftbukkit.util.CraftNamespacedKey.fromMinecraft(dimension.identifier()));
        this.uuid = BukkitWorldIdentity.getUUID(levelSave, this.dimension());
        this.felt$paperConfig = FeltServer.getPaperConfigurations().createWorldConfig(
            io.papermc.paper.configuration.PaperConfigurations.createWorldContextMap(
                BukkitWorldIdentity.getWorldFolder(levelSave, dimension), dimension.identifier(),
                this.spigotConfig, this.registryAccess(), this.getGameRules()));
        for (var category : org.bukkit.entity.SpawnCategory.values()) {
            if (org.bukkit.craftbukkit.util.CraftSpawnCategory.isValidForLimits(category)) {
                this.getWorld().setSpawnLimit(category, this.felt$paperConfig.entities.spawning.spawnLimits
                    .getInt(org.bukkit.craftbukkit.util.CraftSpawnCategory.toNMS(category)));
            }
        }
        ((WorldBorderBridge) this.getWorldBorder()).bridge$setWorld((ServerLevel) (Object) this);
        ((ServerChunkProviderBridge) this.chunkSource).bridge$setViewDistance(spigotConfig.viewDistance);
        ((ServerChunkProviderBridge) this.chunkSource).bridge$setSimulationDistance(spigotConfig.simulationDistance);
        if (felt$isActual()) {
            ((PrimaryLevelDataBridge) this.K).bridge$setWorld((ServerLevel) (Object) this);
            var data = this.getDataStorage().computeIfAbsent(LevelPersistentData.TYPE);
            this.getWorld().readBukkitValues(data.getTag());
            this.getCraftServer().addWorld(this.getWorld());
        }
    }

    @Inject(method = "saveLevelData(Z)V", at = @At("HEAD"))
    private void felt$savePdc(boolean sync, CallbackInfo ci) {
        var data = this.getDataStorage().computeIfAbsent(LevelPersistentData.TYPE);
        data.save(this.world);
    }

    private io.papermc.paper.configuration.WorldConfiguration felt$paperConfig;

    @Override
    public io.papermc.paper.configuration.WorldConfiguration bridge$paperConfig() {
        return this.felt$paperConfig;
    }

    public LevelChunk getChunkIfLoaded(int x, int z) {
        return this.chunkSource.getChunk(x, z, false);
    }

    @Nullable
    @Override
    public ChunkAccess bridge$getChunkIfLoadedImmediately(int x, int z) {
        return this.getChunkSource().getChunkNow(x, z);
    }

    public <T extends ParticleOptions> int sendParticles(final ServerPlayer sender, final T t0, final double d0, final double d1, final double d2, final int i, final double d3, final double d4, final double d5, final double d6, final boolean force) {
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(t0, force, false, d0, d1, d2, (float) d3, (float) d4, (float) d5, (float) d6, i);
        int j = 0;
        for (ServerPlayer entity : this.players) {
            if (sender == null || ((ServerPlayerBridge) entity).bridge$getBukkitEntity().canSee(((ServerPlayerBridge) sender).bridge$getBukkitEntity())) {
                if (this.sendParticles(entity, force, d0, d1, d2, packet)) {
                    ++j;
                }
            }
        }
        return j;
    }

    @Override
    public LevelStorageSource.LevelStorageAccess bridge$getConvertable() {
        return this.storageSource;
    }

    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;commonTick()V", shift = At.Shift.AFTER), require = 1, allow = 1)
    private void felt$tickTotalAge(Entity entity, CallbackInfo ci) {
        entity.totalEntityAge++;
    }

    @Inject(method = "tickPassenger(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;commonTick()V", shift = At.Shift.AFTER), require = 1, allow = 1)
    private void felt$tickPassengerTotalAge(Entity ridingEntity, Entity passengerEntity, CallbackInfo ci) {
        passengerEntity.totalEntityAge++;
    }

    @org.spongepowered.asm.mixin.Unique private boolean felt$activeEntityTick;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
    private void felt$activateEntities(CallbackInfo ci) {
        io.papermc.paper.entity.activation.ActivationRange.activateEntities((ServerLevel) (Object) this);
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "tickNonPassenger")
    private void felt$preserveActivation(Entity entity, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        boolean previous = this.felt$activeEntityTick;
        try {
            original.call(entity);
        } finally {
            this.felt$activeEntityTick = previous;
        }
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tickNonPassenger",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void felt$activationTick(Entity entity, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        this.felt$activeEntityTick = io.papermc.paper.entity.activation.ActivationRange.checkIfActive(entity);
        if (this.felt$activeEntityTick) {
            original.call(entity);
            ((EntityBridge) entity).bridge$postTick();
        } else {
            ((EntityBridge) entity).bridge$inactiveTick();
        }
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tickPassenger",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void felt$activationPassengerTick(Entity entity, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
                                               Entity vehicle, Entity passenger) {
        if (this.felt$activeEntityTick) {
            original.call(entity);
            ((EntityBridge) entity).bridge$postTick();
        } else {
            entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            ((EntityBridge) entity).bridge$inactiveTick();
            vehicle.positionRider(entity);
        }
    }

    @Decorate(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int felt$modifyThunderChance(RandomSource instance, int i) throws Throwable {
        return (int) DecorationOps.callsite().invoke(instance, i == 100000 ? spigotConfig.thunderChance : i);
    }

    @Inject(method = "tickThunder", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void felt$spawnReasonForSkeletonHorse(LevelChunk levelChunk, CallbackInfo ci) {
        bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
    }

    @Decorate(method = "tickThunder", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean felt$spawnReasonForLightning(ServerLevel instance, Entity entity) throws Throwable {
        if (DistValidate.isValid(this)) {
            LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((LightningStrike) entity.bridge$getBukkitEntity(), LightningStrikeEvent.Cause.WEATHER);
            if (lightning.isCancelled()) {
                return false;
            }
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }

    public boolean strikeLightning(Entity entity) {
        return this.strikeLightning(entity, LightningStrikeEvent.Cause.UNKNOWN);
    }

    public boolean strikeLightning(Entity entity, LightningStrikeEvent.Cause cause) {
        if (felt$cause != null) {
            cause = felt$cause;
            felt$cause = null;
        }
        if (DistValidate.isValid(this)) {
            LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((LightningStrike) entity.bridge$getBukkitEntity(), cause);
            if (lightning.isCancelled()) {
                return false;
            }
        }
        return this.addFreshEntity(entity);
    }

    @Decorate(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean felt$snowForm(ServerLevel level, BlockPos pos, BlockState newState) throws Throwable {
        final var event = FeltEventFactory.callBlockFormEvent(level, pos, newState, 3, null);
        if (event != null) {
            if (event.isCancelled()) {
                return false;
            }
            newState = ((CraftBlockState) event.getNewState()).getHandle();
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, newState);
    }

    //TODO: weather cycle for every player

    @Inject(method = "save", at = @At(value = "JUMP", ordinal = 0, opcode = Opcodes.IFNULL))
    private void felt$worldSaveEvent(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (DistValidate.isValid(this)) {
            Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void felt$saveLevelDat(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (!skipSave && this.serverLevelData instanceof PrimaryLevelData worldInfo) {
            var profile = this.getServer().getSingleplayerProfile();
            this.storageSource.saveDataTag(worldInfo, profile == null ? null : profile.id());
        }
    }

    @Decorate(method = {"<init>", "findNearestMapStructure"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldGenSettings()Lnet/minecraft/world/level/levelgen/WorldGenSettings;"))
    private WorldGenSettings felt$worldGenerationSettings(MinecraftServer server) throws Throwable {
        var original = (WorldGenSettings) DecorationOps.callsite().invoke(server);
        var own = WorldCreationContext.lookup((ServerLevel) (Object) this);
        return own != null ? own : original;
    }

    public WorldGenSettings getWorldGenSettings() {
        var own = WorldCreationContext.lookup((ServerLevel) (Object) this);
        return own != null ? own : this.getServer().getWorldGenSettings();
    }

    // Multiworld support: use respective world data
    @Decorate(method = {"isFlat", "enabledFeatures"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldData()Lnet/minecraft/world/level/storage/WorldData;"))
    private WorldData felt$findNearestMapStructure(MinecraftServer instance) throws Throwable {
        return serverLevelData instanceof PrimaryLevelData primary ? primary : (WorldData) DecorationOps.callsite().invoke(instance);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    public void felt$closeOnChunkUnloading(LevelChunk chunkIn, CallbackInfo ci) {
        for (BlockEntity tileentity : chunkIn.getBlockEntities().values()) {
            if (tileentity instanceof Container) {
                for (HumanEntity h : Lists.newArrayList(((IInventoryBridge) tileentity).getViewers())) {
                    if (h instanceof CraftHumanEntity) {
                        ((CraftHumanEntity) h).getHandle().closeContainer();
                    }
                }
            }
        }
    }

    private transient boolean felt$force = false;

    @Decorate(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDIDDDDLnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket$RandomizationType;)I"))
    public int felt$particleVisible(ServerLevel serverWorld, ParticleOptions type, boolean longDistance, boolean alwaysShow, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType randomizationType) throws Throwable {
        try {
            return (int) DecorationOps.callsite().invoke(serverWorld, type, felt$force, alwaysShow, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed, randomizationType);
        } finally {
            felt$force = false;
        }
    }

    public <T extends ParticleOptions> int sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        felt$force = force;
        return this.sendParticles(type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed);
    }

    @Override
    public <T extends ParticleOptions> int bridge$sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        return this.sendParticles(type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed, force);
    }

    private transient LightningStrikeEvent.Cause felt$cause;

    @Override
    public void bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause cause) {
        this.felt$cause = cause;
    }

    @Override
    public void bridge$strikeLightning(LightningBolt entity, LightningStrikeEvent.Cause cause) {
        strikeLightning(entity, cause);
    }

    private transient CreatureSpawnEvent.SpawnReason felt$reason;

    private void felt$clearAddEntityReason() {
        this.felt$reason = null;
    }
    // Fix for SPIGOT-6415
    private transient FeltSpawnReason felt$extendedReason;

    @Inject(method = "addEntity", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private void felt$addEntityEvent(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        // SPIGOT-6415: Don't call spawn event when reason is null.
        final var reason = felt$reason;
        felt$clearAddEntityReason();
        if (felt$extendedReason == FeltSpawnReason.TELEPORT && reason == null) {
            return;
        }
        CreatureSpawnEvent.SpawnReason spawnReason = reason == null ? CreatureSpawnEvent.SpawnReason.DEFAULT : reason;
        if (!felt$isActual()) {
            return;
        }
        ((EntityBridge) entityIn).felt$onAddedToLevel();
        if (!CraftEventFactory.doEntityAddEventCalling((ServerLevel) (Object) this, entityIn, spawnReason)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addEntity", at = @At("RETURN"))
    public void felt$resetReason(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        felt$clearAddEntityReason();
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        this.felt$reason = reason;
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        return this.felt$reason;
    }

    public boolean addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        bridge$pushAddEntityReason(reason);
        return addFreshEntity(entity);
    }

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addFreshEntity(entity, reason);
    }

    public boolean addWithUUID(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        bridge$pushAddEntityReason(reason);
        return addWithUUID(entity);
    }

    @Inject(method = "addDuringTeleport", at = @At("HEAD"))
    private void felt$ignoreSpawnOnTeleport(Entity entity, CallbackInfo ci) {
        felt$extendedReason = FeltSpawnReason.TELEPORT;
    }

    @Inject(method = "addDuringTeleport", at = @At("RETURN"))
    private void felt$unsetIgnoreSpawnOnTeleport(Entity entity, CallbackInfo ci) {
        felt$extendedReason = null;
    }

    public void addDuringTeleport(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity instanceof ServerPlayer player) {
            this.addPlayer(player);
        } else if (reason != null) {
            this.addFreshEntity(entity, reason);
        } else {
            felt$extendedReason = FeltSpawnReason.TELEPORT;
            addFreshEntity(entity);
            felt$extendedReason = null;
        }
    }

    @Override
    public boolean bridge$addEntitySerialized(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addWithUUID(entity, reason);
    }

    public boolean tryAddFreshEntityWithPassengers(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity.getSelfAndPassengers().map(Entity::getUUID).anyMatch(this.entityManager::isLoaded)) {
            return false;
        }
        return this.bridge$addAllEntities(entity, reason);
    }

    @Override
    public boolean bridge$addAllEntitiesSafely(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return tryAddFreshEntityWithPassengers(entity, reason);
    }

    @Decorate(method = "destroyBlockProgress", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<ServerPlayer> felt$limitBreakVisibility(List<ServerPlayer> instance, @Local(ordinal = 0) int id) throws Throwable {
        final var raw = (Iterator<ServerPlayer>) DecorationOps.callsite().invoke(instance);
        final var actor = getEntity(id);
        if (!(actor instanceof ServerPlayerBridge player)) {
            return raw;
        }
        return Iterators.filter(raw, it -> it != null && ((ServerPlayerBridge)it).bridge$getBukkitEntity().canSee(player.bridge$getBukkitEntity()));
    }

    @Decorate(method = "sendBlockUpdated", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"))
    private Object felt$guardConcurrencyOnNavigation(Iterator<PathNavigation> instance, @Local(ordinal = 0) BlockPos pos, @Local(ordinal = 0) BlockState before, @Local(ordinal = 1) BlockState after, @Local(ordinal = 0) int i) throws Throwable {
        try {
            return DecorationOps.callsite().invoke(instance);
        } catch (ConcurrentModificationException ignored) {
            sendBlockUpdated(pos,  before, after, i);
            return DecorationOps.cancel().invoke();
        }
    }

    private boolean[] felt$explosionResult;

    @Override
    public boolean bridge$createExplosion(Entity source, double x, double y, double z, float power, boolean fire, Level.ExplosionInteraction interaction) {
        boolean[] previous = this.felt$explosionResult;
        boolean[] result = new boolean[1];
        this.felt$explosionResult = result;
        try {
            ((ServerLevel) (Object) this).explode(source, Explosion.getDefaultDamageSource((ServerLevel) (Object) this, source), null, x, y, z, power, fire, interaction);
            return result[0];
        } finally {
            this.felt$explosionResult = previous;
        }
    }

    @ModifyVariable(method = "explode", index = 17, at = @At(value = "STORE", ordinal = 0))
    private Explosion.BlockInteraction felt$standardExplodePost(Explosion.BlockInteraction interaction) {
        return super.felt$getStandardExplodeBlockInteraction(interaction);
    }

    @Decorate(method = "explode", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;isSmall()Z"))
    private void felt$doExplosion(@Local(ordinal = 0) ServerExplosion explosion) throws Throwable {
        if (this.felt$explosionResult != null) {
            this.felt$explosionResult[0] = !((ExplosionBridge) explosion).bridge$wasCancelled();
        }
        if (((ExplosionBridge) explosion).bridge$wasCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Inject(method = "getMapData", at = @At("RETURN"))
    private void felt$mapSetId(MapId id, CallbackInfoReturnable<MapItemSavedData> cir) {
        var data = cir.getReturnValue();
        if (data != null) {
            ((MapItemSavedDataBridge) data).bridge$setId(id);
        }
    }

    @Inject(method = "setMapData", at = @At("HEAD"))
    private void felt$mapSetId(MapId id, MapItemSavedData data, CallbackInfo ci) {
        ((MapItemSavedDataBridge) data).bridge$setId(id);
        MapInitializeEvent event = new MapInitializeEvent(((MapItemSavedDataBridge) data).bridge$getMapView());
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    @Inject(method = "updateNeighboursOnBlockSet", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"))
    private void felt$returnIfPopulate(BlockPos pos, BlockState oldState, CallbackInfo ci) {
        if (populating) {
            ci.cancel();
        }
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos, boolean validate) {
        return this.getBlockEntity(pos);
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "tick(Ljava/util/function/BooleanSupplier;)V", require = 1, expect = 1, allow = 1)
    private void felt$nightTickScope(java.util.function.BooleanSupplier timeLeft,
                                        com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        FeltCaptures.nightLevel = this;
        FeltCaptures.nightCancelled = false;
        try {
            original.call(timeLeft);
        } finally {
            FeltCaptures.nightLevel = null;
            FeltCaptures.nightCancelled = false;
        }
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;clockManager()Lnet/minecraft/world/clock/ServerClockManager;"), require = 1, expect = 1, allow = 3)
    private net.minecraft.world.clock.ServerClockManager felt$sleepWorldClock(net.minecraft.world.clock.ServerClockManager global) {
        return ((ServerLevel) (Object) this).clockManager();
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;wakeUpAllPlayers()V"), require = 1, expect = 1, allow = 1)
    private void felt$notWakeIfCancelled(ServerLevel world, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (!(FeltCaptures.nightLevel == world && FeltCaptures.nightCancelled)) {
            original.call(world);
        }
    }

    @Override
    public ServerLevel bridge$getMinecraftWorld() {
        return (ServerLevel) (Object) this;
    }

    @ModifyVariable(method = "tickBlock", ordinal = 0, argsOnly = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos felt$captureTickingBlock(BlockPos pos) {
        FeltCaptures.captureTickingBlock((ServerLevel) (Object) this, pos);
        return pos;
    }

    @ModifyVariable(method = "tickBlock", ordinal = 0, argsOnly = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos felt$resetTickingBlock(BlockPos pos) {
        FeltCaptures.resetTickingBlock();
        return pos;
    }

    @ModifyVariable(method = "tickChunk", ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos felt$captureRandomTick(BlockPos pos) {
        FeltCaptures.captureTickingBlock((ServerLevel) (Object) this, pos);
        return pos;
    }

    @ModifyVariable(method = "tickChunk", ordinal = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos felt$resetRandomTick(BlockPos pos) {
        FeltCaptures.resetTickingBlock();
        return pos;
    }

    @ModifyVariable(method = "tickNonPassenger", argsOnly = true, ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private Entity felt$captureTickingEntity(Entity entity) {
        FeltCaptures.captureTickingEntity(entity);
        return entity;
    }

    @ModifyVariable(method = "tickNonPassenger", argsOnly = true, ordinal = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private Entity felt$resetTickingEntity(Entity entity) {
        FeltCaptures.resetTickingEntity();
        return entity;
    }

    @ModifyVariable(method = "tickPassenger", argsOnly = true, ordinal = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private Entity felt$captureTickingPassenger(Entity entity) {
        FeltCaptures.captureTickingEntity(entity);
        return entity;
    }

    @ModifyVariable(method = "tickPassenger", argsOnly = true, ordinal = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private Entity felt$resetTickingPassenger(Entity entity) {
        FeltCaptures.resetTickingEntity();
        return entity;
    }
}
