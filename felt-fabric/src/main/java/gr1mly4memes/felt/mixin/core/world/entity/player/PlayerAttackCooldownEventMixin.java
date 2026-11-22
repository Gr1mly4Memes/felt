package gr1mly4memes.felt.mixin.core.world.entity.player;

import com.destroystokyo.paper.event.player.PlayerAttackEntityCooldownResetEvent;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerAttackCooldownEventMixin {

    @Unique private Entity felt$cooldownResetTarget;

    @Decorate(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;onAttack()V"), require = 1, allow = 1)
    private void felt$attackCooldownTarget(Player player, Entity target) throws Throwable {
        Entity previousTarget = this.felt$cooldownResetTarget;
        this.felt$cooldownResetTarget = target;
        try {
            // Keep the native virtual call, including its superclass/mod tail.
            DecorationOps.callsite().invoke(player);
        } finally {
            this.felt$cooldownResetTarget = previousTarget;
        }
    }

    @SuppressWarnings("removal")
    @Decorate(method = "onAttack()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;resetOnlyAttackStrengthTicker()V"), require = 1, allow = 1)
    private void felt$attackCooldownReset(Player player) throws Throwable {
        Entity target = this.felt$cooldownResetTarget;
        // Claim before dispatch: an ordinary onAttack() from a listener has no target.
        this.felt$cooldownResetTarget = null;
        if (target == null || player.level().isClientSide()
            || new PlayerAttackEntityCooldownResetEvent(
                (org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
                ((EntityBridge) target).bridge$getBukkitEntity(), player.getAttackStrengthScale(0.0F)).callEvent()) {
            // A veto skips only this reset, not onAttack's superclass continuation.
            DecorationOps.callsite().invoke(player);
        }
    }
}
