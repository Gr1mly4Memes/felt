package gr1mly4memes.felt.mixin.core.world.item;

import gr1mly4memes.felt.mod.util.DistValidate;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {

    private transient ArmorStand felt$entity;

    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;snapTo(DDDFF)V"))
    public void felt$captureEntity(ArmorStand armorStandEntity, double x, double y, double z, float yaw, float pitch) {
        armorStandEntity.snapTo(x, y, z, yaw, pitch);
        felt$entity = armorStandEntity;
    }

    @Decorate(method = "useOn", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    public void felt$entityPlace(UseOnContext context, @Local(ordinal = -1) ArmorStand armorStand) throws Throwable {
        if (DistValidate.isValid(context) && CraftEventFactory.callEntityPlaceEvent(context, armorStand).isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            return;
        }
        DecorationOps.blackhole().invoke();
    }
}
