package gr1mly4memes.felt.mixin.core.network;

import gr1mly4memes.felt.bridge.core.network.ConnectionSpawnLocationEventBridge;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionSpawnLocationEventMixin implements ConnectionSpawnLocationEventBridge {

    @Unique private boolean felt$legacySpawnLocationHandled;

    @Override
    public boolean bridge$isLegacySpawnLocationHandled() {
        return this.felt$legacySpawnLocationHandled;
    }

    @Override
    public void bridge$setLegacySpawnLocationHandled(boolean handled) {
        this.felt$legacySpawnLocationHandled = handled;
    }
}
