package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.mod.util.TreeCapture;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$5")
public abstract class BoneMealDispenseBehaviorMixin extends OptionalDispenseItemBehavior {
    @WrapMethod(method = "execute")
    private ItemStack felt$dispenseBoneMeal(BlockSource source, ItemStack stack, Operation<ItemStack> original) {
        var target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        ItemStack veto = CraftEventFactory.handleBlockDispenseEvent(source, target, stack, this);
        if (veto != null) {
            this.setSuccess(false);
            return veto;
        }
        return TreeCapture.dispenseBoneMeal(source.level(), target, () -> original.call(source, stack));
    }
}
