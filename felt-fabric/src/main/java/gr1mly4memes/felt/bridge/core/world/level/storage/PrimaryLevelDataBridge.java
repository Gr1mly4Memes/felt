package gr1mly4memes.felt.bridge.core.world.level.storage;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelData.RespawnData;

public interface PrimaryLevelDataBridge {
    ResourceKey<Level> bridge$getRespawnDimension();
    void bridge$setRespawnDimension(ResourceKey<Level> dimension);
    void bridge$setVanillaRespawnData(RespawnData data);
    RespawnData bridge$getDimensionSpawn(ResourceKey<Level> dimension);
    void bridge$setDimensionSpawn(ResourceKey<Level> dimension, RespawnData data);

    void bridge$setWorld(ServerLevel world);

    ServerLevel bridge$getWorld();

    LevelSettings bridge$getWorldSettings();

    Lifecycle bridge$getLifecycle();

    void felt$checkName(String name);

    void felt$offerCustomDimensions(Registry<LevelStem> registry);
}
