package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import java.util.List;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$11")
public abstract class BrushDispenseBehaviorMixin extends OptionalDispenseItemBehavior {
    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"), cancellable = true)
    private void felt$beforeBrushing(BlockSource source, ItemStack brush, CallbackInfoReturnable<ItemStack> cir,
                                     @Local List<Armadillo> targets) {
        var selection = DispenserBlockHooks.selectEquipment(source, this, brush, targets.getFirst());
        if (!selection.proceed()) {
            if (selection.event().isCancelled()) {
                this.setSuccess(false);
            }
            cir.setReturnValue(brush);
        }
    }
}
