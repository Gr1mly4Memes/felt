package gr1mly4memes.felt.common.mixin.vanilla.world.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.common.mod.util.FeltCaptures;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityChorusTeleportMixin_Vanilla {
    @WrapMethod(method = "randomTeleport(DDDZ)Z")
    private boolean arclight$chorusTeleportScope(double x, double y, double z, boolean showParticles, Operation<Boolean> original) {
        var previous = FeltCaptures.chorusActive;
        var pending = FeltCaptures.chorusPending;
        FeltCaptures.chorusActive = pending != null && pending.getFirst() == (Object) this ? pending : null;
        if (FeltCaptures.chorusActive != null) FeltCaptures.chorusPending = null;
        try {
            return original.call(x, y, z, showParticles);
        } finally {
            FeltCaptures.chorusActive = previous;
        }
    }
}
