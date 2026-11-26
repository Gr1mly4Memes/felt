package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.EquipmentDispenseItemBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EquipmentDispenseItemBehavior.class)
public abstract class EquipmentDispenseItemBehaviorMixin {
    @WrapOperation(method = "dispenseEquipment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack felt$selectEquipment(ItemStack stack, int amount, Operation<ItemStack> split,
                                                  @Local(argsOnly = true) BlockSource source, @Local LivingEntity target,
                                                  @Share("consumeEquipment") LocalBooleanRef consume,
                                                  @Cancellable CallbackInfoReturnable<Boolean> cir) {
        var call = DispenserBlockHooks.currentDispense();
        var current = call != null && call.source() == source ? call.behavior() : null;
        var selection = DispenserBlockHooks.selectEquipment(source, current, stack.copyWithCount(amount), target);
        if (!selection.proceed()) {
            cir.setReturnValue(!selection.event().isCancelled());
            return ItemStack.EMPTY;
        }
        consume.set(selection.consumeOriginal());
        return CraftItemStack.asNMSCopy(selection.event().getItem());
    }

    @Inject(method = "dispenseEquipment", at = @At("RETURN"))
    private static void felt$consumeEquipment(BlockSource source, ItemStack stack, CallbackInfoReturnable<Boolean> cir,
                                              @Share("consumeEquipment") LocalBooleanRef consume) {
        if (consume.get()) {
            stack.shrink(1);
        }
    }
}
