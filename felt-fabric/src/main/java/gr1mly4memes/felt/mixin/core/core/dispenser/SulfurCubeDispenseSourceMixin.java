package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.SulfurCubeBlockDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SulfurCubeBlockDispenseItemBehavior.class, targets = "net.minecraft.core.dispenser.DispenseItemBehavior$6")
public abstract class SulfurCubeDispenseSourceMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/dispenser/SulfurCubeBlockDispenseItemBehavior;dispenseBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean felt$withDispenseSource(ServerLevel level, BlockPos pos, ItemStack stack, Operation<Boolean> original,
                                            @Local(argsOnly = true) BlockSource source) {
        return DispenserBlockHooks.withSource(source, (DispenseItemBehavior) (Object) this,
            () -> original.call(level, pos, stack));
    }
}
