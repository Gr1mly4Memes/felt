package gr1mly4memes.felt.mixin.core.world.level.chunk.storage;

import ca.spottedleaf.moonrise.patches.chunk_system.io.ChunkSystemRegionFileStorage;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionDataController.ReadData;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO.RegionDataController.WriteData;
import ca.spottedleaf.moonrise.patches.chunk_system.storage.MoonriseRegionFileStorageAccess;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RegionFileStorage.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseRegionFileStorageMixin implements ChunkSystemRegionFileStorage {
    @Unique private final MoonriseRegionFileStorageAccess felt$moonriseStorage = new MoonriseRegionFileStorageAccess((RegionFileStorage) (Object) this);

    @Override public boolean moonrise$doesRegionFileNotExistNoIO(int x, int z) { return this.felt$moonriseStorage.moonrise$doesRegionFileNotExistNoIO(x, z); }
    @Override public RegionFile moonrise$getRegionFileIfLoaded(int x, int z) { return this.felt$moonriseStorage.moonrise$getRegionFileIfLoaded(x, z); }
    @Override public RegionFile moonrise$getRegionFileIfExists(int x, int z) throws IOException { return this.felt$moonriseStorage.moonrise$getRegionFileIfExists(x, z); }
    @Override public WriteData moonrise$startWrite(int x, int z, CompoundTag data) throws IOException { return this.felt$moonriseStorage.moonrise$startWrite(x, z, data); }
    @Override public void moonrise$finishWrite(int x, int z, WriteData data) throws IOException { this.felt$moonriseStorage.moonrise$finishWrite(x, z, data); }
    @Override public ReadData moonrise$readData(int x, int z) throws IOException { return this.felt$moonriseStorage.moonrise$readData(x, z); }
    @Override public CompoundTag moonrise$finishRead(int x, int z, ReadData data) throws IOException { return this.felt$moonriseStorage.moonrise$finishRead(x, z, data); }

    @WrapMethod(method = "getRegionFile")
    private RegionFile felt$lockCache(ChunkPos pos, boolean create, Operation<RegionFile> original) throws IOException {
        synchronized (this) { return original.call(pos, create); }
    }
    @WrapMethod(method = "flush")
    private void felt$lockCacheLifetime(Operation<Void> original) throws IOException {
        synchronized (this) { original.call(); }
    }
    @WrapMethod(method = "close")
    private void felt$closeCache(Operation<Void> original) throws IOException {
        synchronized (this) {
            try { original.call(); }
            finally { ((RegionFileStorage)(Object)this).regionCache.clear(); }
        }
    }
}
