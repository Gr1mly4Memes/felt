package gr1mly4memes.felt.common.mixin.core.world.item;

import gr1mly4memes.felttools.api.FeltPlatform;
import gr1mly4memes.felt.common.mod.mixins.annotation.OnlyInPlatform;
import gr1mly4memes.felt.common.mod.util.FeltCaptures;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyInPlatform({FeltPlatform.VANILLA, FeltPlatform.FABRIC})
@Mixin(TeleportRandomlyConsumeEffect.class)
public class ChorusFruitItemMixin {
    @Decorate(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;randomTeleport(DDDZ)Z"))
    private boolean arclight$randomTeleport(LivingEntity user, double x, double y, double z, boolean showParticles) throws Throwable {
        var previous = FeltCaptures.chorusPending;
        var chorus = com.mojang.datafixers.util.Pair.of(user, new org.apache.commons.lang3.mutable.MutableBoolean());
        FeltCaptures.chorusPending = chorus;
        boolean result;
        try {
            result = (boolean) DecorationOps.callsite().invoke(user, x, y, z, showParticles);
        } finally {
            FeltCaptures.chorusPending = previous;
        }
        // Cancel after releasing the scope: Decorate early-return is bytecode control flow.
        if (chorus.getSecond().booleanValue()) {
            return (boolean) DecorationOps.cancel().invoke(false);
        }
        return result;
    }
}
