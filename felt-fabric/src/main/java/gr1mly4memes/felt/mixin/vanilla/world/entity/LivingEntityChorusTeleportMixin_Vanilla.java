package gr1mly4memes.felt.mixin.vanilla.world.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityChorusTeleportMixin_Vanilla {
    @WrapMethod(method = "randomTeleport(DDDZLjava/util/function/Predicate;)Z")
    private boolean felt$chorusTeleportScope(double x, double y, double z, boolean showParticles, java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> isInvalidPosition, Operation<Boolean> original) {
        var previous = FeltCaptures.chorusActive;
        var pending = FeltCaptures.chorusPending;
        FeltCaptures.chorusActive = pending != null && pending.getFirst() == (Object) this ? pending : null;
        if (FeltCaptures.chorusActive != null) FeltCaptures.chorusPending = null;
        try {
            return original.call(x, y, z, showParticles, isInvalidPosition);
        } finally {
            FeltCaptures.chorusActive = previous;
        }
    }
}
