package gr1mly4memes.felt.mixin.core.world.entity.ai.village.poi;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.entity.ai.village.poi.*;

@Mixin(PoiManager.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonrisePoiManagerMixin implements ca.spottedleaf.moonrise.patches.chunk_system.level.poi.ChunkSystemPoiManager {
    @Shadow private boolean isVillageCenter(long pos) { throw new AssertionError(); }
    @Shadow public abstract void checkConsistencyWithBlocks(SectionPos pos, LevelChunkSection section);
    @Unique private net.minecraft.server.level.ServerLevel world;

    // the vanilla tracker needs to be replaced because it does not support level removes, and we need level removes
    // to support poi unloading
    @Unique private final ca.spottedleaf.moonrise.common.misc.Delayed26WayDistancePropagator3D villageDistanceTracker = new ca.spottedleaf.moonrise.common.misc.Delayed26WayDistancePropagator3D();

    @Unique private static final int POI_DATA_SOURCE = 7;

    @Unique private static int convertBetweenLevels(final int level) {
        return POI_DATA_SOURCE - level;
    }

    @Unique private void updateDistanceTracking(long section) {
        if (this.isVillageCenter(section)) {
            this.villageDistanceTracker.setSource(section, POI_DATA_SOURCE);
        } else {
            this.villageDistanceTracker.removeSource(section);
        }
    }

    public Optional<PoiSection> get(final long pos) {
        final int chunkX = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionX(pos);
        final int chunkY = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionY(pos);
        final int chunkZ = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionZ(pos);

        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.world, chunkX, chunkZ, "Accessing poi chunk off-main");

        final ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk ret = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.world).moonrise$getChunkTaskScheduler().chunkHolderManager.getPoiChunkIfLoaded(chunkX, chunkZ, true);

        return ret == null ? Optional.empty() : ret.getSectionForVanilla(chunkY);
    }

    public Optional<PoiSection> getOrLoad(final long pos) {
        final int chunkX = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionX(pos);
        final int chunkY = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionY(pos);
        final int chunkZ = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionZ(pos);

        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.world, chunkX, chunkZ, "Accessing poi chunk off-main");

        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager manager = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.world).moonrise$getChunkTaskScheduler().chunkHolderManager;

        if (chunkY >= ca.spottedleaf.moonrise.common.util.WorldUtil.getMinSection(this.world) && chunkY <= ca.spottedleaf.moonrise.common.util.WorldUtil.getMaxSection(this.world)) {
            final ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk ret = manager.getPoiChunkIfLoaded(chunkX, chunkZ, true);
            if (ret != null) {
                return ret.getSectionForVanilla(chunkY);
            } else {
                return manager.loadPoiChunk(chunkX, chunkZ).getSectionForVanilla(chunkY);
            }
        }
        // retain vanilla behavior: do not load section if out of bounds!
        return Optional.empty();
    }

    // SectionStorage's generic R erases to Object. Match that descriptor so inherited calls dispatch here.
    protected Object getOrCreate(final long pos) {
        final int chunkX = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionX(pos);
        final int chunkY = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionY(pos);
        final int chunkZ = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionZ(pos);

        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.world, chunkX, chunkZ, "Accessing poi chunk off-main");

        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager manager = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.world).moonrise$getChunkTaskScheduler().chunkHolderManager;

        final ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk ret = manager.getPoiChunkIfLoaded(chunkX, chunkZ, true);
        if (ret != null) {
            return ret.getOrCreateSection(chunkY);
        } else {
            return manager.loadPoiChunk(chunkX, chunkZ).getOrCreateSection(chunkY);
        }
    }

    @Override
    public final net.minecraft.server.level.ServerLevel moonrise$getWorld() {
        return this.world;
    }

    @Override
    public final void moonrise$onUnload(final long coordinate) { // Paper - rewrite chunk system
        final int chunkX = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkX(coordinate);
        final int chunkZ = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkZ(coordinate);

        final int minY = ca.spottedleaf.moonrise.common.util.WorldUtil.getMinSection(this.world);
        final int maxY = ca.spottedleaf.moonrise.common.util.WorldUtil.getMaxSection(this.world);

        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.world, chunkX, chunkZ, "Unloading poi chunk off-main");
        for (int sectionY = minY; sectionY <= maxY; ++sectionY) {
            final long sectionPos = SectionPos.asLong(chunkX, sectionY, chunkZ);
            this.updateDistanceTracking(sectionPos);
        }
    }

    @Override
    public final void moonrise$loadInPoiChunk(final ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk poiChunk) {
        final int chunkX = poiChunk.chunkX;
        final int chunkZ = poiChunk.chunkZ;

        final int minY = ca.spottedleaf.moonrise.common.util.WorldUtil.getMinSection(this.world);
        final int maxY = ca.spottedleaf.moonrise.common.util.WorldUtil.getMaxSection(this.world);

        ca.spottedleaf.moonrise.common.util.TickThread.ensureTickThread(this.world, chunkX, chunkZ, "Loading poi chunk off-main");
        for (int sectionY = minY; sectionY <= maxY; ++sectionY) {
            final PoiSection section = poiChunk.getSection(sectionY);
            if (section != null && !((ca.spottedleaf.moonrise.patches.chunk_system.level.poi.ChunkSystemPoiSection)section).moonrise$isEmpty()) {
                this.onSectionLoad(SectionPos.asLong(chunkX, sectionY, chunkZ));
            }
        }
    }

    @Override
    public final void moonrise$checkConsistency(final net.minecraft.world.level.chunk.ChunkAccess chunk) {
        final int chunkX = chunk.getPos().x();
        final int chunkZ = chunk.getPos().z();

        final int minY = ca.spottedleaf.moonrise.common.util.WorldUtil.getMinSection(chunk);
        final int maxY = ca.spottedleaf.moonrise.common.util.WorldUtil.getMaxSection(chunk);
        final LevelChunkSection[] sections = chunk.getSections();
        for (int section = minY; section <= maxY; ++section) {
            this.checkConsistencyWithBlocks(SectionPos.of(chunkX, section, chunkZ), sections[section - minY]);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$world(net.minecraft.world.level.chunk.storage.RegionStorageInfo info, java.nio.file.Path path, com.mojang.datafixers.DataFixer fixer, boolean sync, net.minecraft.core.RegistryAccess registries, net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter reporter, net.minecraft.world.level.LevelHeightAccessor height, CallbackInfo ci) {
        this.world = (net.minecraft.server.level.ServerLevel) height;
    }
    @Overwrite public int sectionsToVillage(final SectionPos sectionPos) {
        // Paper start - rewrite chunk system
        this.villageDistanceTracker.propagateUpdates();
        return convertBetweenLevels(this.villageDistanceTracker.getLevel(ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionKey(sectionPos)));
        // Paper end - rewrite chunk system
    }

    @Overwrite
    public void tick(final BooleanSupplier haveTime) {
        this.villageDistanceTracker.propagateUpdates(); // Paper - rewrite chunk system
    }

    @Overwrite
    public void setDirty(final long sectionPos) { // Paper - public
        // Paper start - rewrite chunk system
        final int chunkX = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionX(sectionPos);
        final int chunkZ = ca.spottedleaf.moonrise.common.util.CoordinateUtils.getChunkSectionZ(sectionPos);
        final ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager manager = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel)this.world).moonrise$getChunkTaskScheduler().chunkHolderManager;
        final ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk chunk = manager.getPoiChunkIfLoaded(chunkX, chunkZ, false);
        if (chunk != null) {
            chunk.setDirty(true);
        }
        this.updateDistanceTracking(sectionPos);
        // Paper end - rewrite chunk system
    }

    @Overwrite
    protected void onSectionLoad(final long sectionPos) {
        this.updateDistanceTracking(sectionPos); // Paper - rewrite chunk system
    }


}
