package gr1mly4memes.felt.mixin.optimization.general;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

@Mixin(Entity.class)
public abstract class EntityMixin_Optimize {

    @Shadow public ImmutableList<Entity> passengers;

    @Inject(method = "getIndirectPassengersStream", cancellable = true, at = @At("HEAD"))
    private void felt$emptyPassenger(CallbackInfoReturnable<Stream<Entity>> cir) {
        if (this.passengers.isEmpty()) {
            cir.setReturnValue(Stream.empty());
        }
    }

    @Overwrite
    public Iterable<Entity> getIndirectPassengers() {
        if (this.passengers.isEmpty()) {
            return Collections.emptyList();
        } else {
            var list = new ArrayList<Entity>();
            for (var entity : this.passengers) {
                list.add(entity);
                list.addAll((Collection<? extends Entity>) entity.getIndirectPassengers());
            }
            return list;
        }
    }
}
