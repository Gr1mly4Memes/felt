package gr1mly4memes.felt.mixin.core.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import gr1mly4memes.felt.bridge.core.world.level.storage.DerivedLevelDataBridge;
import gr1mly4memes.felt.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelData.RespawnData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataMixin implements PrimaryLevelDataBridge {

    // @formatter:off
    @Shadow public abstract String getLevelName();
    @Shadow public abstract boolean isDifficultyLocked();
    @Shadow public LevelSettings settings;
    @Shadow @Final private Lifecycle worldGenSettingsLifecycle;
    @Shadow public abstract RespawnData getRespawnData();
    // @formatter:on

    public ServerLevel world;
    public Registry<LevelStem> customDimensions;

    @Unique private static final Codec<Map<ResourceKey<Level>, RespawnData>> felt$dimensionSpawnsCodec =
        Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, RespawnData.CODEC);
    @Unique private final Map<ResourceKey<Level>, RespawnData> felt$dimensionSpawns = new HashMap<>();
    @Unique private ResourceKey<Level> felt$respawnDimension;
    @Unique private RespawnData felt$vanillaSpawn;

    @Override
    public ResourceKey<Level> bridge$getRespawnDimension() {
        return this.felt$respawnDimension == null ? this.getRespawnData().dimension() : this.felt$respawnDimension;
    }

    @Override
    public void bridge$setRespawnDimension(ResourceKey<Level> dimension) {
        this.felt$respawnDimension = dimension;
    }

    @Override
    public void bridge$setVanillaRespawnData(RespawnData data) {
        this.felt$vanillaSpawn = data;
    }

    @Override
    public RespawnData bridge$getDimensionSpawn(ResourceKey<Level> dimension) {
        return this.felt$dimensionSpawns.get(dimension);
    }

    @Override
    public void bridge$setDimensionSpawn(ResourceKey<Level> dimension, RespawnData data) {
        this.felt$dimensionSpawns.put(dimension, data);
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void felt$loadSpawnSettings(Dynamic<?> input, LevelSettings settings, PrimaryLevelData.SpecialWorldProperty property,
                                              Lifecycle lifecycle, CallbackInfoReturnable<PrimaryLevelData> cir) {
        var data = cir.getReturnValue();
        var bridge = (PrimaryLevelDataBridge) data;
        bridge.bridge$setRespawnDimension(input.get("paperSpawnDimension").read(Level.RESOURCE_KEY_CODEC)
            .result().orElse(data.getRespawnData().dimension()));
        input.get("Felt.LocalSpawn").read(RespawnData.CODEC).result().ifPresent(data::setSpawn);
        input.get("Felt.DimensionSpawns").read(felt$dimensionSpawnsCodec).result()
            .ifPresent(spawns -> spawns.forEach(bridge::bridge$setDimensionSpawn));
    }

    @Inject(method = "setTagData", at = @At("RETURN"))
    private void felt$saveSpawnSettings(CompoundTag tag, UUID profile, CallbackInfo ci) {
        tag.store("paperSpawnDimension", Level.RESOURCE_KEY_CODEC, this.bridge$getRespawnDimension());
        if (!this.felt$dimensionSpawns.isEmpty()) {
            tag.store("Felt.DimensionSpawns", felt$dimensionSpawnsCodec, this.felt$dimensionSpawns);
        }
        if (this.felt$vanillaSpawn != null && !this.felt$vanillaSpawn.equals(this.getRespawnData())) {
            tag.store("Felt.LocalSpawn", RespawnData.CODEC, this.getRespawnData());
            tag.store("spawn", RespawnData.CODEC, this.felt$vanillaSpawn);
        }
    }

    @Inject(method = "setDifficulty", at = @At("RETURN"))
    private void felt$sendDiffChange(Difficulty newDifficulty, CallbackInfo ci) {
        ClientboundChangeDifficultyPacket packet = new ClientboundChangeDifficultyPacket(newDifficulty, this.isDifficultyLocked());
        for (ServerPlayer player : this.world.getServer().getPlayerList().getPlayers()) {
            if (felt$ownsDifficulty(player.level().getLevelData())) {
                player.connection.send(packet);
            }
        }
    }

    private boolean felt$ownsDifficulty(LevelData data) {
        if (data == (Object) this) {
            return true;
        }
        if (data instanceof DerivedLevelDataBridge bridge) {
            return bridge.bridge$getWorldData() == (Object) this;
        }
        return false;
    }

    @Override
    public void bridge$setWorld(ServerLevel world) {
        setWorld(world);
    }

    public void setWorld(ServerLevel world) {
        if (this.world == null) {
            this.world = world;
        }
    }

    @Override
    public ServerLevel bridge$getWorld() {
        return world;
    }

    public void checkName(String name) {
        if (!this.settings.levelName.equals(name)) {
            this.settings.levelName = name;
        }
    }

    @Override
    public void felt$checkName(String name) {
        checkName(name);
    }

    @Override
    public void felt$offerCustomDimensions(Registry<LevelStem> registry) {
        this.customDimensions = registry;
    }

    @Override
    public LevelSettings bridge$getWorldSettings() {
        return this.settings;
    }

    @Override
    public Lifecycle bridge$getLifecycle() {
        return this.worldGenSettingsLifecycle;
    }
}
