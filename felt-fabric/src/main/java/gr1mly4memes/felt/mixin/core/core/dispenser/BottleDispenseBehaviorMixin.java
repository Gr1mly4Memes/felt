package gr1mly4memes.felt.mixin.core.core.dispenser;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.core.dispenser.DispenseItemBehavior$9", "net.minecraft.core.dispenser.DispenseItemBehavior$13"})
public abstract class BottleDispenseBehaviorMixin {
    @Inject(method = "execute", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BeehiveBlock;releaseBeesAndResetHoneyLevel(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$BeeReleaseStatus;)V"),
        @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior$9;setSuccess(Z)V", ordinal = 2),
        @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isClientSide()Z")
    }, cancellable = true, require = 1)
    private void felt$beforeBottleInteraction(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        var target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        ItemStack veto = CraftEventFactory.handleBlockDispenseEvent(source, target, stack, (DispenseItemBehavior) (Object) this);
        if (veto != null) {
            cir.setReturnValue(veto);
        }
    }
}
