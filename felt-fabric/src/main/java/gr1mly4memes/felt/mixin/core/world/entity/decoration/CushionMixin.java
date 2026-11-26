package gr1mly4memes.felt.mixin.core.world.entity.decoration;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Cushion.class)
public abstract class CushionMixin extends BlockAttachedEntityMixin {
    @WrapOperation(method = "thunderHit", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/decoration/Cushion;kill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"),
        require = 1)
    private void felt$rememberLightningDamage(Cushion cushion, ServerLevel level, Entity lightning, Operation<Void> original) {
        this.felt$setDecorationDeathSource(cushion.damageSources().lightningBolt());
        original.call(cushion, level, lightning);
    }
}
