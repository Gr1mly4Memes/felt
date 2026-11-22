package gr1mly4memes.felt.mixin.core.world.entity.decoration;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceKnotEntityMixin {

    @Decorate(method = "interact", require = 2, allow = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;canHaveALeashAttachedTo(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean felt$allowLeashTransfer(Leashable leashable, Entity holder, @Local(ordinal = 0) Player player, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        boolean allowed = (boolean) DecorationOps.callsite().invoke(leashable, holder);
        return allowed && CraftEventFactory.handlePlayerLeashEntityEvent(leashable, holder, player, hand);
    }

    @Inject(method = "notifyLeasheeRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/LeashFenceKnotEntity;discard()V"))
    private void felt$removeCause(Leashable entity, CallbackInfo ci) {
        ((EntityBridge) this).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
    }
}
