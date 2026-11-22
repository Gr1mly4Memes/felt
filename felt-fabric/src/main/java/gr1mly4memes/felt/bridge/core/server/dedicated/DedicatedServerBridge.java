package gr1mly4memes.felt.bridge.core.server.dedicated;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;

public interface DedicatedServerBridge {
    net.minecraft.server.dedicated.DedicatedServerProperties bridge$reloadProperties();
    String bridge$runRconCommand(net.minecraft.server.rcon.RconConsoleSource source, String command);

    default void bridge$platform$exitNow() {
    }

    WorldLoader.DataLoadContext felt$dataLoadContext();

    void felt$forceUpgradeIfNeeded(LevelStorageSource.LevelStorageAccess worldSession, WorldData worldData, RegistryAccess.Frozen dimensions);

    void felt$prepareAndAddLevel(ServerLevel level, PrimaryLevelData levelData);
}
