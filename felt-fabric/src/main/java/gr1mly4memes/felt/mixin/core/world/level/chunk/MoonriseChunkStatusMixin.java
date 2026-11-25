package gr1mly4memes.felt.mixin.core.world.level.chunk;

import ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemChunkStatus;
import gr1mly4memes.felt.bridge.core.world.chunk.MoonriseChunkStatusLinkBridge;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkStatus.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseChunkStatusMixin implements ChunkSystemChunkStatus, MoonriseChunkStatusLinkBridge {
    @Unique private boolean felt$parallel;
    @Unique private boolean felt$emptyLoad;
    @Unique private int felt$writeRadius = -1;
    @Unique private ChunkStatus felt$next;
    @Unique private final AtomicBoolean felt$warned = new AtomicBoolean();
    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$link(ChunkStatus parent, EnumSet<Heightmap.Types> heightmaps, ChunkType type, CallbackInfo ci) {
        this.felt$next = (ChunkStatus) (Object) this;
        if (parent != null) ((MoonriseChunkStatusLinkBridge) (Object) parent).felt$setNextStatus(this.felt$next);
    }
    @Override public void felt$setNextStatus(ChunkStatus status) { this.felt$next = status; }
    @Override public boolean moonrise$isParallelCapable() { return this.felt$parallel; }
    @Override public void moonrise$setParallelCapable(boolean value) { this.felt$parallel = value; }
    @Override public int moonrise$getWriteRadius() { return this.felt$writeRadius; }
    @Override public void moonrise$setWriteRadius(int value) { this.felt$writeRadius = value; }
    @Override public ChunkStatus moonrise$getNextStatus() { return this.felt$next; }
    @Override public boolean moonrise$isEmptyLoadStatus() { return this.felt$emptyLoad; }
    @Override public void moonrise$setEmptyLoadStatus(boolean value) { this.felt$emptyLoad = value; }
    @Override public boolean moonrise$isEmptyGenStatus() { return (Object) this == ChunkStatus.EMPTY; }
    @Override public AtomicBoolean moonrise$getWarnedAboutNoImmediateComplete() { return this.felt$warned; }
}
