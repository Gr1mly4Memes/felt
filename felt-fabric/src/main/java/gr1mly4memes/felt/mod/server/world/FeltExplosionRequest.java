package gr1mly4memes.felt.mod.server.world;

import net.minecraft.world.level.ServerExplosion;

public final class FeltExplosionRequest {
    public final boolean excludeSource;
    public ServerExplosion explosion;

    public FeltExplosionRequest(boolean excludeSource) {
        this.excludeSource = excludeSource;
    }
}
