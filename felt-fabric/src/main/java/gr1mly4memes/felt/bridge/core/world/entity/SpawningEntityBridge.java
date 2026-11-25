package gr1mly4memes.felt.bridge.core.world.entity;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface SpawningEntityBridge {
    @Nullable UUID bridge$getSpawningEntity();
    void bridge$setSpawningEntity(@Nullable UUID spawningEntity);
}
