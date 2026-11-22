package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.bridge.core.server.level.DistanceManagerBridge;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import gr1mly4memes.felttools.mixin.Decorate;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DistanceManager.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class DistanceManagerQueuedUpdatesMixin {
    @Decorate(method = "runAllUpdates", inject = true, at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"))
    private void felt$runQueuedUpdates(ChunkMap map) {
        ((DistanceManagerBridge) this).bridge$runQueuedUpdates(map);
    }
}
