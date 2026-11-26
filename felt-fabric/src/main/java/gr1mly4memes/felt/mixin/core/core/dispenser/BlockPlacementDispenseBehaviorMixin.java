package gr1mly4memes.felt.mixin.core.core.dispenser;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Wither skull, carved pumpkin and honeycomb. Their fallback still uses the native behavior.
@Mixin(targets = {"net.minecraft.core.dispenser.DispenseItemBehavior$7", "net.minecraft.core.dispenser.DispenseItemBehavior$8",
    "net.minecraft.core.dispenser.DispenseItemBehavior$12"})
public abstract class BlockPlacementDispenseBehaviorMixin extends OptionalDispenseItemBehavior {
    @Inject(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
        cancellable = true)
    private void felt$beforeDispensedPlacement(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        var target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        ItemStack veto = CraftEventFactory.handleBlockDispenseEvent(source, target, stack, this);
        if (veto != null) {
            this.setSuccess(false);
            cir.setReturnValue(veto);
        }
    }
}
