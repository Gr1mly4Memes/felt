package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(net.minecraft.server.level.ServerPlayer.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseServerPlayerMixin implements ca.spottedleaf.moonrise.patches.chunk_system.player.ChunkSystemServerPlayer {
    @Unique private boolean isRealPlayer;
    @Unique private ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.PlayerChunkLoaderData chunkLoader;
    @Unique private final ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.ViewDistanceHolder viewDistanceHolder = new ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.ViewDistanceHolder();

    @Override
    public final boolean moonrise$isRealPlayer() {
        return this.isRealPlayer;
    }

    @Override
    public final void moonrise$setRealPlayer(final boolean real) {
        this.isRealPlayer = real;
    }

    @Override
    public final ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.PlayerChunkLoaderData moonrise$getChunkLoader() {
        return this.chunkLoader;
    }

    @Override
    public final void moonrise$setChunkLoader(final ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.PlayerChunkLoaderData loader) {
        this.chunkLoader = loader;
    }

    @Override
    public final ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader.ViewDistanceHolder moonrise$getViewDistanceHolder() {
        return this.viewDistanceHolder;
    }

}
