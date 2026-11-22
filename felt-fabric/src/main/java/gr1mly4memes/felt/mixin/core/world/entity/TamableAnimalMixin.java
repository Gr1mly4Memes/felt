package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.mixin.core.world.entity.animal.AnimalMixin;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityToggleSitEvent;
import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.Location;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin extends AnimalMixin {

    // @formatter:off
    @Shadow public abstract boolean isTame();

    @org.spongepowered.asm.mixin.injection.Inject(method = "feed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;heal(F)V"))
    private void felt$foodHeal(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.bridge$pushHealReason(org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.EATING);
    }

    // @formatter:on

    @Unique private boolean felt$skipSitEvent;

    @Decorate(method = "setInSittingPose(Z)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void felt$toggleSit(boolean value) throws Throwable {
        // NBT restoration bypasses only this setter entry, not later nested setters.
        boolean callEvent = !this.felt$skipSitEvent;
        this.felt$skipSitEvent = false;
        if (callEvent && !new EntityToggleSitEvent(this.getBukkitEntity(), value).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Decorate(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;setInSittingPose(Z)V"), require = 1, allow = 1)
    private void felt$restoreSittingPose(TamableAnimal animal, boolean value) throws Throwable {
        boolean previous = this.felt$skipSitEvent;
        this.felt$skipSitEvent = true;
        try {
            DecorationOps.callsite().invoke(animal, value);
        } finally {
            this.felt$skipSitEvent = previous;
        }
    }

    @Decorate(method = "maybeTeleportTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;snapTo(DDDFF)V"))
    private void felt$teleportEvent(TamableAnimal instance, double x, double y, double z, float yaw, float pitch) throws Throwable {
        EntityTeleportEvent event = CraftEventFactory.callEntityTeleportEvent(instance, x, y, z);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
        Location to = event.getTo();
        DecorationOps.callsite().invoke(instance, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
    }
}
