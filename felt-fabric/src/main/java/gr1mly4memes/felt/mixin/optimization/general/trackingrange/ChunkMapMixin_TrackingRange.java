package gr1mly4memes.felt.mixin.optimization.general.trackingrange;

import gr1mly4memes.felt.mod.config.FeltConfig;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import org.spigotmc.TrackingRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkMap.class)
public class ChunkMapMixin_TrackingRange {

    @Unique
    private static final boolean felt$applyTrackingRange = FeltConfig.useActivationAndTrackingRange;

    @ModifyVariable(method = "addEntity", index = 3, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;updateInterval()I"))
    private int trackingRange$updateRange(int defaultRange, Entity entity) {
        return felt$applyTrackingRange ? TrackingRange.getEntityTrackingRange(entity, defaultRange) : defaultRange;
    }
}
