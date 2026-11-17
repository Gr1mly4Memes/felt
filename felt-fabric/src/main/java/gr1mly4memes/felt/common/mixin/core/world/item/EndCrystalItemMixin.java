package gr1mly4memes.felt.common.mixin.core.world.item;

import gr1mly4memes.felt.common.mod.util.DistValidate;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndCrystalItem.class)
public class EndCrystalItemMixin {

    @Decorate(method = "useOn", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$entityPlace(UseOnContext context, @Local(ordinal = -1) EndCrystal enderCrystalEntity) throws Throwable {
        if (DistValidate.isValid(context) && CraftEventFactory.callEntityPlaceEvent(context, enderCrystalEntity).isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            return;
        }
        DecorationOps.blackhole().invoke();
    }
}
