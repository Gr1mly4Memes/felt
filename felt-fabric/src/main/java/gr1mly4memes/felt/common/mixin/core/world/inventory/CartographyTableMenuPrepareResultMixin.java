package gr1mly4memes.felt.common.mixin.core.world.inventory;

import gr1mly4memes.felttools.mixin.Decorate;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuPrepareResultMixin {

    @Shadow @Final private ResultContainer resultContainer;

    @Decorate(method = "slotsChanged(Lnet/minecraft/world/Container;)V", inject = true,
        at = @At("TAIL"), require = 1, allow = 1)
    private void arclight$prepareCartographyResult(Container changed) {
        // Result writes are not new input calculations. Preserve the existing
        // click-result guard and avoid turning an edited result into another event.
        if (changed != this.resultContainer) {
            CraftEventFactory.callPrepareResultEvent((CartographyTableMenu) (Object) this, CartographyTableMenu.RESULT_SLOT);
        }
    }
}
