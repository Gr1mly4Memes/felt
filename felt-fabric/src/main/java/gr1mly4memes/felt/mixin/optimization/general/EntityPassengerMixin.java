package gr1mly4memes.felt.mixin.optimization.general;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

/**
 * Paper - Optimize indirect passenger iteration. Short-circuits the stream/iterable
 * allocations when an entity has no passengers (the common case).
 */
@Mixin(Entity.class)
public abstract class EntityPassengerMixin {

    @Shadow private ImmutableList<Entity> passengers;

    @Inject(method = "getIndirectPassengersStream", at = @At("HEAD"), cancellable = true)
    private void felt$emptyIndirectPassengersStream(CallbackInfoReturnable<Stream<Entity>> cir) {
        if (this.passengers.isEmpty()) {
            cir.setReturnValue(Stream.of());
        }
    }

    @Inject(method = "getSelfAndPassengers", at = @At("HEAD"), cancellable = true)
    private void felt$emptySelfAndPassengers(CallbackInfoReturnable<Stream<Entity>> cir) {
        if (this.passengers.isEmpty()) {
            cir.setReturnValue(Stream.of((Entity) (Object) this));
        }
    }

    @Inject(method = "getPassengersAndSelf", at = @At("HEAD"), cancellable = true)
    private void felt$emptyPassengersAndSelf(CallbackInfoReturnable<Stream<Entity>> cir) {
        if (this.passengers.isEmpty()) {
            cir.setReturnValue(Stream.of((Entity) (Object) this));
        }
    }

    @Inject(method = "hasExactlyOnePlayerPassenger", at = @At("HEAD"), cancellable = true)
    private void felt$emptyHasExactlyOnePlayerPassenger(CallbackInfoReturnable<Boolean> cir) {
        if (this.passengers.isEmpty()) {
            cir.setReturnValue(false);
        }
    }
}
