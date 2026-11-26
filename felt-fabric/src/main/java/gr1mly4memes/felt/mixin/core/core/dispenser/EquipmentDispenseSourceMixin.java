package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.EquipmentDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EquipmentDispenseItemBehavior.class,
    targets = {"net.minecraft.core.dispenser.DispenseItemBehavior$7", "net.minecraft.core.dispenser.DispenseItemBehavior$8"})
public abstract class EquipmentDispenseSourceMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/dispenser/EquipmentDispenseItemBehavior;dispenseEquipment(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean felt$withEquipmentBehavior(BlockSource source, ItemStack stack, Operation<Boolean> original) {
        return DispenserBlockHooks.withSource(source, (DispenseItemBehavior) (Object) this, () -> original.call(source, stack));
    }
}
