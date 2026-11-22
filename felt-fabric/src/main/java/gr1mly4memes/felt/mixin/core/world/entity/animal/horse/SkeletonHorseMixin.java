package gr1mly4memes.felt.mixin.core.world.entity.animal.horse;

import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonHorse.class)
public abstract class SkeletonHorseMixin extends AbstractHorseMixin {

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/SkeletonHorse;discard()V"))
    private void felt$despawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }
}
