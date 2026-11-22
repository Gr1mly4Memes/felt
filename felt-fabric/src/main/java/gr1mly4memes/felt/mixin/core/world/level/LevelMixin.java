package gr1mly4memes.felt.mixin.core.world.level;

import gr1mly4memes.felt.bridge.core.world.level.LevelAccessorBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.bridge.core.world.level.border.WorldBorderBridge;
import gr1mly4memes.felt.mod.FeltConstants;
import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.TransformAccess;
import gr1mly4memes.felt.mod.server.world.FeltWorldConfig;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felt.mod.util.DistValidate;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.BitRandomSource;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CapturedBlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.CraftSpawnCategory;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.generator.ChunkGenerator;
import org.objectweb.asm.Opcodes;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Mixin(Level.class)
public abstract class LevelMixin implements WorldBridge, gr1mly4memes.felt.bridge.core.world.level.TreeCaptureBridge, LevelAccessor, LevelWriter {
    public int wakeupInactiveRemainingAnimals;
    @Override public int bridge$getWakeupInactiveAnimals() { return this.wakeupInactiveRemainingAnimals; }
    @Override public void bridge$setWakeupInactiveAnimals(int value) { this.wakeupInactiveRemainingAnimals = value; }

    public int wakeupInactiveRemainingFlying;
    @Override public int bridge$getWakeupInactiveFlying() { return this.wakeupInactiveRemainingFlying; }
    @Override public void bridge$setWakeupInactiveFlying(int value) { this.wakeupInactiveRemainingFlying = value; }

    public int wakeupInactiveRemainingMonsters;
    @Override public int bridge$getWakeupInactiveMonsters() { return this.wakeupInactiveRemainingMonsters; }
    @Override public void bridge$setWakeupInactiveMonsters(int value) { this.wakeupInactiveRemainingMonsters = value; }

    public int wakeupInactiveRemainingVillagers;
    @Override public int bridge$getWakeupInactiveVillagers() { return this.wakeupInactiveRemainingVillagers; }
    @Override public void bridge$setWakeupInactiveVillagers(int value) { this.wakeupInactiveRemainingVillagers = value; }


    // @formatter:off
    @Shadow @Nullable public BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Shadow public abstract BlockState getBlockState(BlockPos pos);
    @Shadow public abstract MinecraftServer getServer();
    @Shadow public abstract LevelData getLevelData();
    @Shadow public abstract ResourceKey<Level> dimension();
    @Shadow public abstract DimensionType dimensionType();
    @Shadow public abstract void setBlocksDirty(BlockPos arg, BlockState arg2, BlockState arg3);
    @Shadow @Final public boolean isClientSide;
    @Shadow public abstract void sendBlockUpdated(BlockPos arg, BlockState arg2, BlockState arg3, int i);
    @Shadow public abstract void updateNeighbourForOutputSignal(BlockPos arg, Block arg2);
    @Shadow public abstract void neighborChanged(BlockState state, BlockPos pos, Block block, net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston);
    @Shadow public abstract void updatePOIOnBlockStateChange(BlockPos arg, BlockState arg2, BlockState arg3);
    @Shadow public abstract RegistryAccess registryAccess();
    @Accessor("thread") public abstract Thread felt$getMainThread();
    // @formatter:on

    protected CraftWorld world;

    public boolean pvpMode;

    public final Object2LongOpenHashMap<SpawnCategory> ticksPerSpawnCategory = new Object2LongOpenHashMap<>();

    public boolean populating;

    public org.bukkit.generator.ChunkGenerator generator;
    protected org.bukkit.World.Environment environment;
    protected org.bukkit.generator.BiomeProvider biomeProvider;

    public org.spigotmc.SpigotWorldConfig spigotConfig;
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static BlockPos lastPhysicsProblem; // Spigot
    public boolean preventPoiUpdated = false;

    public volatile boolean captureTreeGeneration;

    public Map<BlockPos, CapturedBlockState> capturedBlockStates = new java.util.LinkedHashMap<>();

    public Map<BlockPos, BlockEntity> capturedTileEntities = new HashMap<>();

    @Unique private boolean felt$isActual;
    @Unique private final gr1mly4memes.felt.mod.util.TreeCapture felt$treeCapture = new gr1mly4memes.felt.mod.util.TreeCapture();

    @Override
    public Object bridge$treeCaptureIdentity() {
        if (!this.felt$isActual || Thread.currentThread() != this.felt$getMainThread()) {
            throw new IllegalStateException("Tree capture identity requires the logic Level thread");
        }
        return this.felt$treeCapture.currentIdentity();
    }

    @Override
    public <T> gr1mly4memes.felt.mod.util.TreeCapture.Result<T> bridge$captureTree(java.util.function.Supplier<T> action) {
        if (!this.felt$isActual) {
            throw new IllegalStateException("Tree capture requires an actual logic Level");
        }
        if (Thread.currentThread() != this.felt$getMainThread()) {
            throw new IllegalStateException("Tree capture requires the Level thread");
        }
        boolean previous = this.captureTreeGeneration;
        this.captureTreeGeneration = true;
        try {
            return this.felt$treeCapture.capture(action);
        } finally {
            this.captureTreeGeneration = previous;
        }
    }

    @Inject(method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("HEAD"), cancellable = true)
    private void felt$treeRead(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (this.felt$isActual && Thread.currentThread() == this.felt$getMainThread()) {
            BlockState state = this.felt$treeCapture.read(pos);
            if (state != null) cir.setReturnValue(state);
        }
    }

    @Override
    @Nullable
    public BlockState bridge$getBlockStateIfLoaded(BlockPos pos) {
        if (this.felt$isActual && Thread.currentThread() == this.felt$getMainThread()) {
            BlockState state = this.felt$treeCapture.read(pos);
            if (state != null) {
                return state;
            }
        }
        if (this.isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        ChunkAccess chunk = ((LevelAccessorBridge) this).getChunkIfLoadedImmediately(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    @ShadowConstructor
    public void felt$constructor(WritableLevelData worldInfo, ResourceKey<Level> dimension, RegistryAccess registryAccess, final Holder<DimensionType> dimensionType, boolean isRemote, boolean isDebug, long seed, int maxNeighborUpdate) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void felt$constructor(WritableLevelData worldInfo, ResourceKey<Level> dimension, RegistryAccess registryAccess, final Holder<DimensionType> dimensionType, boolean isRemote, boolean isDebug, long seed, int maxNeighborUpdate, org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider, org.bukkit.World.Environment env) {
        felt$constructor(worldInfo, dimension, registryAccess, dimensionType, isRemote, isDebug, seed, maxNeighborUpdate);
        this.generator = gen;
        this.environment = env;
        this.biomeProvider = biomeProvider;
    }

    @SuppressWarnings({"DefaultAnnotationParam", "UnnecessaryUnsafe"})
    // InitAuther97: unsafe = true is required because unsafe defaults to false
    @Inject(method = "<init>", at = @At(value = "CTOR_HEAD", unsafe = true))
    private void felt$preInit(WritableLevelData writableLevelData, ResourceKey resourceKey, RegistryAccess registryAccess, Holder holder, boolean bl, boolean bl2, long l, int i, CallbackInfo ci) {
        this.felt$isActual = DistValidate.isValid((LevelAccessor) (Object) this);
    }

    @Override
    public Map<BlockPos, CapturedBlockState> bridge$getCapturedBlockState() {
        return this.capturedBlockStates;
    }

    @Override
    public boolean felt$isActual() {
        return felt$isActual;
    }

    @Override
    public Map<BlockPos, BlockEntity> bridge$getCapturedBlockEntity() {
        return this.capturedTileEntities;
    }

    @Override
    public void bridge$setLastPhysicsProblem(BlockPos pos) {
        lastPhysicsProblem = pos;
    }

    @Override
    public Object2LongOpenHashMap<SpawnCategory> bridge$ticksPerSpawnCategory() {
        return this.ticksPerSpawnCategory;
    }

    public abstract ResourceKey<LevelStem> getTypeKey();

    @Override
    public ResourceKey<LevelStem> bridge$getTypeKey() {
        return getTypeKey();
    }

    @Override
    public SpigotWorldConfig bridge$spigotConfig() {
        if (spigotConfig == null) {
            return FeltWorldConfig.DEFAULT;
        }
        return this.spigotConfig;
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
        at = @At("HEAD"), cancellable = true)
    private void felt$hooks(BlockPos pos, BlockState newState, int flags, int i, CallbackInfoReturnable<Boolean> cir) {
        if (this.felt$isActual && Thread.currentThread() == this.felt$getMainThread()
            && this.felt$treeCapture.active()) {
            this.felt$treeCapture.write((Level) (Object) this, pos, newState, flags);
            cir.setReturnValue(true);
            return;
        }
        if (!FeltCaptures.setLastEntityChangeBlockResult(processCaptures(pos, newState, flags))) {
            cir.setReturnValue(false);
        }
    }

    private boolean processCaptures(BlockPos pos, BlockState newState, int flags) {
        Entity entityChangeBlock = FeltCaptures.getEntityChangeBlock();
        if (entityChangeBlock != null) {
            return CraftEventFactory.callEntityChangeBlockEvent(entityChangeBlock, pos, newState);
        }
        return true;
    }

    public void notifyAndUpdatePhysics(BlockPos pos, LevelChunk chunk, BlockState oldBlock, BlockState newBlock, BlockState actualBlock, int i, int j) {
        this.bridge$mod$notifyAndUpdatePhysics(pos, chunk, oldBlock, newBlock, i, j);
    }

    @Override
    public void bridge$mod$notifyAndUpdatePhysics(BlockPos pos, @Nullable LevelChunk levelchunk, BlockState oldBlock, BlockState newBlock, int j, int k) {
        Block block = newBlock.getBlock();
        BlockState blockstate1 = this.getBlockState(pos);
        if (blockstate1 == newBlock) {
            if (oldBlock != blockstate1) {
                this.setBlocksDirty(pos, oldBlock, blockstate1);
            }

            if ((j & 2) != 0 && (!this.isClientSide || (j & 4) == 0) && (this.isClientSide || levelchunk == null || levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
                this.sendBlockUpdated(pos, oldBlock, newBlock, j);
            }

            if ((j & 1) != 0) {
                this.neighborChanged(blockstate1, pos, oldBlock.getBlock(), null, false);
                if (!this.isClientSide && newBlock.hasAnalogOutputSignal()) {
                    this.updateNeighbourForOutputSignal(pos, block);
                }
            }

            if ((j & 16) == 0 && k > 0) {
                int i = j & -34;
                oldBlock.updateIndirectNeighbourShapes(this, pos, i, k - 1);
                try {
                    if (this.world != null) {
                        BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at(this, pos), CraftBlockData.createData(newBlock));
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            return;
                        }
                    }
                } catch (StackOverflowError e) {
                    lastPhysicsProblem = pos;
                }
                newBlock.updateNeighbourShapes(this, pos, i, k - 1);
                newBlock.updateIndirectNeighbourShapes(this, pos, i, k - 1);
            }

            if (!this.preventPoiUpdated && (j & gr1mly4memes.felt.mod.FeltConstants.UPDATE_SKIP_POI) == 0) {
                this.updatePOIOnBlockStateChange(pos, oldBlock, blockstate1);
            }
        }
    }

    public CraftServer getCraftServer() {
        return (CraftServer) Bukkit.getServer();
    }

    public CraftWorld getWorld() {
        if (this.world == null) {
            throw new UnsupportedOperationException(String.format("World %s does not have a CraftWorld. This method should not be invoked on this world for any reason.", dimension().registry()));
        }
        return this.world;
    }

    public BlockEntity getBlockEntity(BlockPos pos, boolean validate) {
        return getBlockEntity(pos);
    }

    @Override
    public BlockEntity bridge$getTileEntity(BlockPos pos, boolean validate) {
        return getBlockEntity(pos, validate);
    }

    @Override
    public CraftServer bridge$getServer() {
        return (CraftServer) Bukkit.getServer();
    }

    @Override
    public CraftWorld bridge$getWorld() {
        return this.getWorld();
    }

    @Override
    public boolean bridge$isPvpMode() {
        return this.pvpMode;
    }

    @Override
    public boolean bridge$isPopulating() {
        return this.populating;
    }

    @Override
    public void bridge$setPopulating(boolean populating) {
        this.populating = populating;
    }

    @Override
    public ChunkGenerator bridge$getGenerator() {
        return generator;
    }

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (getWorld().getHandle() != (Object) this) {
            return ((WorldBridge) getWorld().getHandle()).bridge$addEntity(entity, reason);
        } else {
            this.bridge$pushAddEntityReason(reason);
            return this.addFreshEntity(entity);
        }
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        if (getWorld().getHandle() != (Object) this) {
            ((WorldBridge) getWorld().getHandle()).bridge$pushAddEntityReason(reason);
        }
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        if (getWorld().getHandle() != (Object) this) {
            return ((WorldBridge) getWorld().getHandle()).bridge$getAddEntityReason();
        }
        return null;
    }

    @Override
    public boolean bridge$preventPoiUpdated() {
        return this.preventPoiUpdated;
    }

    @Override
    public void bridge$preventPoiUpdated(boolean b) {
        this.preventPoiUpdated = b;
    }

    protected transient Explosion.BlockInteraction felt$blockInteractionOverride;

    @ModifyVariable(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V",
        index = 12, at = @At("HEAD"), argsOnly = true)
    private Level.ExplosionInteraction felt$standardExplodePre(Level.ExplosionInteraction interaction) {
        if (interaction == FeltConstants.STANDARD) {
            felt$blockInteractionOverride = Explosion.BlockInteraction.DESTROY;
            return Level.ExplosionInteraction.BLOCK;
        }
        return interaction;
    }

    protected Explosion.BlockInteraction felt$getStandardExplodeBlockInteraction(Explosion.BlockInteraction interaction) {
        try {
            if (felt$blockInteractionOverride != null) {
                return felt$blockInteractionOverride;
            } else {
                return interaction;
            }
        } finally {
            felt$blockInteractionOverride = null;
        }
    }

    @Override
    public <T> gr1mly4memes.felt.mod.util.TreeCapture.JoinResult<T> bridge$captureTreeOrJoin(java.util.function.Supplier<T> action) {
        if (!this.felt$isActual) {
            throw new IllegalStateException("Tree capture requires an actual logic Level");
        }
        if (Thread.currentThread() != this.felt$getMainThread()) {
            throw new IllegalStateException("Tree capture requires the Level thread");
        }
        boolean previous = this.captureTreeGeneration;
        this.captureTreeGeneration = true;
        try {
            return this.felt$treeCapture.captureOrJoin(action);
        } finally {
            this.captureTreeGeneration = previous;
        }
    }

    public io.papermc.paper.configuration.WorldConfiguration paperConfig() {
        return this.bridge$paperConfig();
    }


}
