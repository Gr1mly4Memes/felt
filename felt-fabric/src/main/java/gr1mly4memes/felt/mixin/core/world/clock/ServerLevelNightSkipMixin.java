package gr1mly4memes.felt.mixin.core.world.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felttools.api.FeltPlatform;
import gr1mly4memes.felt.mod.mixins.annotation.OnlyInPlatform;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyInPlatform(FeltPlatform.FABRIC)
@Mixin(ServerLevel.class)
public abstract class ServerLevelNightSkipMixin {
    @WrapOperation(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;moveToTimeMarker(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/clock/ServerClockManager$MoveResult;"), require = 1, expect = 1, allow = 1)
    private ServerClockManager.MoveResult felt$nightMarker(ServerClockManager manager, Holder<WorldClock> clock,
                                         ResourceKey<ClockTimeMarker> marker, Operation<ServerClockManager.MoveResult> original) {
        FeltCaptures.nightManager = manager;
        FeltCaptures.nightClock = clock;
        try {
            return original.call(manager, clock, marker);
        } finally {
            FeltCaptures.nightManager = null;
            FeltCaptures.nightClock = null;
        }
    }
}
