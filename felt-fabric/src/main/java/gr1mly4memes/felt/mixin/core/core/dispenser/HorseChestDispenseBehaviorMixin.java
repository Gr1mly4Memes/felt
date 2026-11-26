package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$2")
public abstract class HorseChestDispenseBehaviorMixin extends OptionalDispenseItemBehavior {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/SlotAccess;set(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean felt$selectChest(SlotAccess slot, ItemStack stack, Operation<Boolean> set,
                                     @Local(argsOnly = true) BlockSource source, @Local AbstractChestedHorse horse,
                                     @Share("consumeChest") LocalBooleanRef consume,
                                     @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        var selection = DispenserBlockHooks.selectEquipment(source, this, stack.copyWithCount(1), horse);
        if (!selection.proceed()) {
            if (selection.event().isCancelled()) {
                this.setSuccess(false);
            }
            cir.setReturnValue(stack);
            return false;
        }
        consume.set(selection.consumeOriginal());
        set.call(slot, CraftItemStack.asNMSCopy(selection.event().getItem()));
        return true;
    }

    @WrapWithCondition(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private boolean felt$consumeChest(ItemStack stack, int amount, @Share("consumeChest") LocalBooleanRef consume) {
        return consume.get();
    }
}
