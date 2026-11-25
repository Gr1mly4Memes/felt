package gr1mly4memes.felt.mixin.core.world.level.chunk.storage;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Native Paper chunk system binding; the external Moonrise mod owns its own bindings. */
@Mixin(net.minecraft.world.level.chunk.storage.IOWorker.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseIOWorkerMixin implements gr1mly4memes.felt.bridge.core.world.chunk.MoonriseIOWorkerBridge {
    @Shadow @Final private net.minecraft.world.level.chunk.storage.RegionFileStorage storage;
    @Override public net.minecraft.world.level.chunk.storage.RegionFileStorage felt$getRegionStorage() { return this.storage; }
}
