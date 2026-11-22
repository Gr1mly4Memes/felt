package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felttools.mixin.Decorate;
import net.minecraft.world.inventory.StonecutterMenu;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuPrepareResultMixin {

    @Decorate(method = "slotsChanged(Lnet/minecraft/world/Container;)V", inject = true,
        at = @At("RETURN"), require = 1, allow = 1)
    private void felt$prepareStonecutterResult() {
        // Match Paper's slotsChanged completion, including an unchanged input
        // type. Selection/replenishment paths retain their native result logic.
        CraftEventFactory.callPrepareResultEvent((StonecutterMenu) (Object) this, StonecutterMenu.RESULT_SLOT);
    }
}
