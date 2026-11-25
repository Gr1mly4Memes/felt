package gr1mly4memes.felt.mixin.core.world.level.chunk.storage;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Native Paper chunk system binding; the external Moonrise mod owns its own bindings. */
@Mixin(net.minecraft.world.level.chunk.storage.SimpleRegionStorage.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseSimpleRegionStorageMixin implements ca.spottedleaf.moonrise.patches.chunk_system.storage.ChunkSystemSimpleRegionStorage {
    @Shadow @Final private net.minecraft.world.level.chunk.storage.IOWorker worker;
    @Override public net.minecraft.world.level.chunk.storage.RegionFileStorage moonrise$getRegionStorage() {
        return ((gr1mly4memes.felt.bridge.core.world.chunk.MoonriseIOWorkerBridge) this.worker).felt$getRegionStorage();
    }
}
