package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityUseItemEventMixin {

    @Decorate(method = "releaseUsingItem()V", require = 1, allow = 1, inject = true,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;useItem:Lnet/minecraft/world/item/ItemStack;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void felt$stopUsingItemEvent() {
        if ((Object) this instanceof ServerPlayer) {
            LivingEntity entity = (LivingEntity) (Object) this;
            // The held-item match has succeeded and useItem now refers to the
            // current held stack. Keep mod loader's stop hook and native release next.
            new PlayerStopUsingItemEvent(
                (Player) ((LivingEntityBridge) this).bridge$getBukkitEntity(),
                CraftItemStack.asCraftMirror(entity.getUseItem()), entity.getTicksUsingItem()
            ).callEvent();
        }
    }
}
