package gr1mly4memes.felt.mixin.core.world.level.chunk.storage;

import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionDataController.WriteData;
import ca.spottedleaf.moonrise.patches.chunk_system.storage.ChunkSystemRegionFile;
import ca.spottedleaf.moonrise.patches.chunk_system.storage.MoonriseRegionFileAccess;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RegionFile.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseRegionFileMixin implements ChunkSystemRegionFile, MoonriseRegionFileAccess.Owner {
    @Unique private final MoonriseRegionFileAccess felt$moonriseFile = new MoonriseRegionFileAccess((RegionFile) (Object) this);

    @Override public MoonriseRegionFileAccess moonrise$getRegionFileAccess() { return this.felt$moonriseFile; }
    @Override public int moonrise$getRecalculateCount() { return this.felt$moonriseFile.moonrise$getRecalculateCount(); }
    @Override public boolean moonrise$recalculateHeader() throws IOException { return this.felt$moonriseFile.moonrise$recalculateHeader(); }
    @Override public Boolean moonrise$tryRecalcHeader(int count) throws IOException { return this.felt$moonriseFile.moonrise$tryRecalcHeader(count); }
    @Override public WriteData moonrise$startWrite(CompoundTag data, ChunkPos pos) throws IOException {
        return this.felt$moonriseFile.moonrise$startWrite(data, pos);
    }

    // Paper protects header access, mutation and file lifetime with the same region monitor.
    @WrapMethod(method = {"doesChunkExist", "hasChunk"})
    private boolean felt$lockHeaderRead(ChunkPos pos, Operation<Boolean> original) {
        synchronized (this) { return original.call(pos); }
    }
    @WrapMethod(method = "clear")
    private void felt$lockClear(ChunkPos pos, Operation<Void> original) throws IOException {
        synchronized (this) { original.call(pos); }
    }
    @WrapMethod(method = "close")
    private void felt$lockLifetime(Operation<Void> original) throws IOException {
        this.felt$moonriseFile.runClose(ignored -> original.call());
    }
    @WrapMethod(method = "flush")
    private void felt$lockFlush(Operation<Void> original) throws IOException {
        synchronized (this) { original.call(); }
    }
}
