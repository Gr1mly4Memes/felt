package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.SulfurCubeBlockDispenseItemBehavior;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SulfurCubeBlockDispenseItemBehavior.class)
public abstract class SulfurCubeBlockDispenseItemBehaviorMixin {
    @WrapOperation(method = "dispenseBlock", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;equipItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean felt$dispenseToCube(SulfurCube cube, ItemStack stack, Operation<Boolean> original,
                                               @Local(argsOnly = true) BlockPos pos,
                                               @Cancellable CallbackInfoReturnable<Boolean> cir) {
        var call = DispenserBlockHooks.currentDispense();
        if (call == null) {
            return original.call(cube, stack);
        }
        if (cube.isBaby() || stack.is(cube.getItemBySlot(EquipmentSlot.BODY).getItem())) {
            return false;
        }
        var event = CraftEventFactory.handleBlockDispenseEventInspectable(call.source(), pos, stack, call.behavior());
        if (event.result.preventBehaviour()) {
            // Suppress both stack shrink here and the invoking behavior's fallback.
            cir.setReturnValue(true);
            return false;
        }
        original.call(cube, CraftItemStack.asNMSCopy(event.getItem()));
        return true;
    }
}
