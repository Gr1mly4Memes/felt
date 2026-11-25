package gr1mly4memes.felt.mixin.core.world.level.chunk.storage;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.nbt.CompoundTag;
import ca.spottedleaf.moonrise.patches.starlight.storage.StarlightSectionData;
import ca.spottedleaf.moonrise.patches.starlight.util.SaveUtil;

@Mixin(SerializableChunkData.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseSerializableChunkDataMixin {
    @Shadow @Final private List<SerializableChunkData.SectionData> sectionData;
    @Shadow @Final @Mutable private boolean lightCorrect;
    @Unique private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    @Unique private ProtoChunk loadStarlightLightData(final ServerLevel world, final ProtoChunk ret) {

        final boolean hasSkyLight = world.dimensionType().hasSkyLight();
        final int minSection = ca.spottedleaf.moonrise.common.util.WorldUtil.getMinLightSection(world);

        final ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray[] blockNibbles = ca.spottedleaf.moonrise.patches.starlight.light.StarLightEngine.getFilledEmptyLight(world);
        final ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray[] skyNibbles = ca.spottedleaf.moonrise.patches.starlight.light.StarLightEngine.getFilledEmptyLight(world);

        if (!this.lightCorrect) {
            ((ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk)ret).starlight$setBlockNibbles(blockNibbles);
            ((ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk)ret).starlight$setSkyNibbles(skyNibbles);
            return ret;
        }

        try {
            for (final SerializableChunkData.SectionData sectionData : this.sectionData) {
                final int y = sectionData.y();
                final DataLayer blockLight = sectionData.blockLight();
                final DataLayer skyLight = sectionData.skyLight();

                final int blockState = ((ca.spottedleaf.moonrise.patches.starlight.storage.StarlightSectionData)(Object)sectionData).starlight$getBlockLightState();
                final int skyState = ((ca.spottedleaf.moonrise.patches.starlight.storage.StarlightSectionData)(Object)sectionData).starlight$getSkyLightState();

                if (blockState >= 0) {
                    if (blockLight != null) {
                        blockNibbles[y - minSection] = new ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray(ca.spottedleaf.moonrise.common.util.MixinWorkarounds.clone(blockLight.getData()), blockState); // clone for data safety
                    } else {
                        blockNibbles[y - minSection] = new ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray(null, blockState);
                    }
                }

                if (skyState >= 0 && hasSkyLight) {
                    if (skyLight != null) {
                        skyNibbles[y - minSection] = new ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray(ca.spottedleaf.moonrise.common.util.MixinWorkarounds.clone(skyLight.getData()), skyState); // clone for data safety
                    } else {
                        skyNibbles[y - minSection] = new ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray(null, skyState);
                    }
                }
            }

            ((ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk)ret).starlight$setBlockNibbles(blockNibbles);
            ((ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk)ret).starlight$setSkyNibbles(skyNibbles);
        } catch (final Throwable thr) {
            ret.setLightCorrect(false);

            LOGGER.error("Failed to parse light data for chunk " + ret.getPos() + " in world '" + ca.spottedleaf.moonrise.common.util.WorldUtil.getWorldName(world) + "'", thr);
        }

        return ret;
    }

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void felt$snapshot(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
        List<SerializableChunkData.SectionData> sectionData = cir.getReturnValue().sectionData();
        sectionData.clear();
        final int minLightSection = ca.spottedleaf.moonrise.common.util.WorldUtil.getMinLightSection(level);
        final int maxLightSection = ca.spottedleaf.moonrise.common.util.WorldUtil.getMaxLightSection(level);
        final int minBlockSection = ca.spottedleaf.moonrise.common.util.WorldUtil.getMinSection(level);

        final boolean copy = !ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder.DO_NOT_COPY_SAVED_DATA.get().booleanValue();

        final LevelChunkSection[] chunkSections = chunk.getSections();
        final ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray[] blockNibbles = ((ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk)chunk).starlight$getBlockNibbles();
        final ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray[] skyNibbles = ((ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk)chunk).starlight$getSkyNibbles();

        for (int lightSection = minLightSection; lightSection <= maxLightSection; ++lightSection) {
            final int lightSectionIdx = lightSection - minLightSection;
            final int blockSectionIdx = lightSection - minBlockSection;

            final LevelChunkSection chunkSection = (blockSectionIdx >= 0 && blockSectionIdx < chunkSections.length) ? (copy ? chunkSections[blockSectionIdx].copy() : chunkSections[blockSectionIdx]) : null;
            final ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray.SaveState blockNibble = blockNibbles[lightSectionIdx].getSaveState();
            final ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray.SaveState skyNibble = skyNibbles[lightSectionIdx].getSaveState();

            if (chunkSection == null && blockNibble == null && skyNibble == null) {
                continue;
            }

            final SerializableChunkData.SectionData section = new SerializableChunkData.SectionData(
                lightSection, chunkSection,
                blockNibble == null ? null : (blockNibble.data == null ? null : new DataLayer(blockNibble.data)),
                skyNibble == null ? null : (skyNibble.data == null ? null : new DataLayer(skyNibble.data))
            );

            if (blockNibble != null) {
                ((ca.spottedleaf.moonrise.patches.starlight.storage.StarlightSectionData)(Object)section).starlight$setBlockLightState(blockNibble.state);
            }

            if (skyNibble != null) {
                ((ca.spottedleaf.moonrise.patches.starlight.storage.StarlightSectionData)(Object)section).starlight$setSkyLightState(skyNibble.state);
            }

            sectionData.add(section);
        }

    }
    @Inject(method = "parse", at = @At("RETURN"))
    private static void felt$parse(net.minecraft.world.level.LevelHeightAccessor height, net.minecraft.world.level.chunk.PalettedContainerFactory registries, CompoundTag tag, CallbackInfoReturnable<SerializableChunkData> cir) {
        SerializableChunkData data = cir.getReturnValue();
        if (data == null) return;
        ((MoonriseSerializableChunkDataMixin)(Object)data).lightCorrect = data.chunkStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.LIGHT)
            && tag.contains("isLightOn") && tag.getIntOr(SaveUtil.STARLIGHT_VERSION_TAG, -1) == SaveUtil.STARLIGHT_LIGHT_VERSION;
        for (net.minecraft.nbt.Tag raw : tag.getListOrEmpty("sections")) {
            if (!(raw instanceof CompoundTag section)) continue;
            int y = section.getByteOr("Y", (byte)0);
            for (SerializableChunkData.SectionData value : data.sectionData()) {
                if (value.y() != y) continue;
                StarlightSectionData light = (StarlightSectionData)(Object)value;
                if (section.contains(SaveUtil.BLOCKLIGHT_STATE_TAG)) light.starlight$setBlockLightState(section.getIntOr(SaveUtil.BLOCKLIGHT_STATE_TAG, 0));
                if (section.contains(SaveUtil.SKYLIGHT_STATE_TAG)) light.starlight$setSkyLightState(section.getIntOr(SaveUtil.SKYLIGHT_STATE_TAG, 0));
            }
        }
    }
    @Inject(method = "write", at = @At("RETURN"))
    private void felt$write(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        tag.putBoolean("isLightOn", false);
        if (this.lightCorrect) tag.putInt(SaveUtil.STARLIGHT_VERSION_TAG, SaveUtil.STARLIGHT_LIGHT_VERSION);
        for (net.minecraft.nbt.Tag raw : tag.getListOrEmpty("sections")) {
            if (!(raw instanceof CompoundTag section)) continue;
            int y = section.getByteOr("Y", (byte)0);
            for (SerializableChunkData.SectionData value : this.sectionData) {
                if (value.y() != y) continue;
                StarlightSectionData light = (StarlightSectionData)(Object)value;
                if (light.starlight$getBlockLightState() > 0) section.putInt(SaveUtil.BLOCKLIGHT_STATE_TAG, light.starlight$getBlockLightState());
                if (light.starlight$getSkyLightState() > 0) section.putInt(SaveUtil.SKYLIGHT_STATE_TAG, light.starlight$getSkyLightState());
            }
        }
    }
    @Inject(method = "read", at = @At("RETURN"))
    private void felt$read(ServerLevel level, net.minecraft.world.entity.ai.village.poi.PoiManager poi, net.minecraft.world.level.chunk.storage.RegionStorageInfo info, net.minecraft.world.level.ChunkPos pos, CallbackInfoReturnable<ProtoChunk> cir) {
        this.loadStarlightLightData(level, cir.getReturnValue());
    }
    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;checkConsistencyWithBlocks(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/chunk/LevelChunkSection;)V"))
    private void felt$deferPoi(net.minecraft.world.entity.ai.village.poi.PoiManager poi, net.minecraft.core.SectionPos pos, LevelChunkSection section) {
        // Full conversion loads and validates POI on the owning tick thread.
    }

}
