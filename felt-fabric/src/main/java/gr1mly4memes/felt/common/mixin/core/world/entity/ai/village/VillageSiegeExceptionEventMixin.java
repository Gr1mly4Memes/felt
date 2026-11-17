package gr1mly4memes.felt.common.mixin.core.world.entity.ai.village;

import com.destroystokyo.paper.exception.ServerInternalException;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.ai.village.VillageSiege;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VillageSiege.class)
public abstract class VillageSiegeExceptionEventMixin {

    @Decorate(method = "trySpawn(Lnet/minecraft/server/level/ServerLevel;)V", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private void arclight$siegeSpawnException(Logger logger, String message, Object spawnPos, Object cause) throws Throwable {
        DecorationOps.callsite().invoke(logger, message, spawnPos, cause);
        try {
            ServerInternalException.reportInternalException((Throwable) cause);
        } catch (Throwable ignored) {
            // Preserve the caller's existing failed-spawn return.
        }
    }
}
