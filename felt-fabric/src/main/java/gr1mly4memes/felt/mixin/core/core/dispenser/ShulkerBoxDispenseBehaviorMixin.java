package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxDispenseBehavior.class)
public abstract class ShulkerBoxDispenseBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "NEW",
        target = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/context/DirectionalPlaceContext;"))
    private DirectionalPlaceContext felt$selectShulkerBox(Level level, BlockPos target, Direction facing, ItemStack stack,
                                                         Direction clickedFace, Operation<DirectionalPlaceContext> create,
                                                         @Local(argsOnly = true) BlockSource source,
                                                         @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        var selection = DispenserBlockHooks.selectDispensedItem(source, (ShulkerBoxDispenseBehavior) (Object) this, stack,
            new Vector(target.getX(), target.getY(), target.getZ()));
        if (!selection.proceed()) {
            cir.setReturnValue(stack);
            return null;
        }
        return create.call(level, target, facing, CraftItemStack.asNMSCopy(selection.event().getItem()), clickedFace);
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/BlockItem;place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult felt$consumePlacedShulkerBox(BlockItem item, BlockPlaceContext context,
                                                           Operation<InteractionResult> original, @Local(argsOnly = true) ItemStack stack) {
        InteractionResult result = original.call(item, context);
        if (result.consumesAction()) {
            stack.shrink(1);
        }
        return result;
    }
}
