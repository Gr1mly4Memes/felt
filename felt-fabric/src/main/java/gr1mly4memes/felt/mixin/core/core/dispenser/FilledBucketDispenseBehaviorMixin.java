package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftVector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$3")
public abstract class FilledBucketDispenseBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/DispensibleContainerItem;emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)Z"))
    private boolean felt$selectBucket(DispensibleContainerItem bucket, LivingEntity actor, Level level, BlockPos target,
                                      BlockHitResult hit, Operation<Boolean> empty, @Local(argsOnly = true) BlockSource source,
                                      @Local(argsOnly = true) ItemStack stack, @Share("bucketItem") LocalRef<ItemStack> chosen,
                                      @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        if (!felt$canEmpty(bucket, level, target)) {
            return empty.call(bucket, actor, level, target, hit);
        }
        var selection = DispenserBlockHooks.selectDispensedItem(source, (DispenseItemBehavior) (Object) this, stack,
            CraftVector.toBukkit(target));
        if (!selection.proceed()) {
            cir.setReturnValue(stack);
            return false;
        }
        var selected = CraftItemStack.asNMSCopy(selection.event().getItem());
        if (!(selected.getItem() instanceof DispensibleContainerItem replacement)) {
            cir.setReturnValue(stack);
            return false;
        }
        chosen.set(selected);
        return empty.call(replacement, actor, level, target, hit);
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/DispensibleContainerItem;checkExtraContent(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/BlockPos;)V"))
    private void felt$releaseBucketContents(DispensibleContainerItem bucket, LivingEntity actor, Level level, ItemStack stack,
                                            BlockPos target, Operation<Void> release, @Share("bucketItem") LocalRef<ItemStack> chosen) {
        ItemStack selected = chosen.get();
        if (selected == null) {
            release.call(bucket, actor, level, stack, target);
        } else {
            release.call((DispensibleContainerItem) selected.getItem(), actor, level, selected, target);
        }
    }

    @Unique
    private static boolean felt$canEmpty(DispensibleContainerItem bucket, Level level, BlockPos target) {
        var state = level.getBlockState(target);
        if (bucket instanceof SolidBucketItem) {
            return level.isInWorldBounds(target) && state.isAir();
        }
        return bucket instanceof BucketItem fluidBucket && fluidBucket.content instanceof FlowingFluid fluid
            && (state.isAir() || state.canBeReplaced(fluid)
                || state.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(null, level, target, state, fluid));
    }
}
