package gr1mly4memes.felt.bridge.core.world.level.storage;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;

public interface PrimaryLevelDataBridge {

    void bridge$setWorld(ServerLevel world);

    ServerLevel bridge$getWorld();

    LevelSettings bridge$getWorldSettings();

    Lifecycle bridge$getLifecycle();

    void felt$checkName(String name);

    void felt$offerCustomDimensions(Registry<LevelStem> registry);
}
