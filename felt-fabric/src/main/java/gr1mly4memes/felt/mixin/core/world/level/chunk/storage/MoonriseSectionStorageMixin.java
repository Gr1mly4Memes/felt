package gr1mly4memes.felt.mixin.core.world.level.chunk.storage;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Native Paper chunk system binding; the external Moonrise mod owns its own bindings. */
@Mixin(net.minecraft.world.level.chunk.storage.SectionStorage.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseSectionStorageMixin implements ca.spottedleaf.moonrise.patches.chunk_system.level.storage.ChunkSystemSectionStorage {
    @Shadow @Final private net.minecraft.world.level.chunk.storage.SimpleRegionStorage simpleRegionStorage;
    @Override public net.minecraft.world.level.chunk.storage.RegionFileStorage moonrise$getRegionStorage() {
        return ((ca.spottedleaf.moonrise.patches.chunk_system.storage.ChunkSystemSimpleRegionStorage) this.simpleRegionStorage).moonrise$getRegionStorage();
    }
    @Override public void moonrise$close() throws java.io.IOException { this.simpleRegionStorage.close(); }
}
