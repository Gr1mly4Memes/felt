package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.DummyLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$4")
public abstract class EmptyBucketDispenseBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack felt$previewPickup(BucketPickup bucket, LivingEntity actor, LevelAccessor level, BlockPos target,
                                         BlockState state, Operation<ItemStack> pickup, @Local(argsOnly = true) BlockSource source,
                                         @Local(argsOnly = true) ItemStack stack, @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        ItemStack preview = pickup.call(bucket, actor, DummyLevelAccessor.INSTANCE, target, state);
        if (!preview.isEmpty()) {
            ItemStack veto = CraftEventFactory.handleBlockDispenseEvent(source, target, stack, (DispenseItemBehavior) (Object) this);
            if (veto != null) {
                cir.setReturnValue(veto);
            }
        }
        return preview;
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/LevelAccessor;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V"))
    private void felt$commitPickup(LevelAccessor level, Entity actor, Holder<GameEvent> event, BlockPos target,
                                   Operation<Void> emit, @Local BucketPickup bucket, @Local BlockState state) {
        emit.call(level, actor, event, target);
        bucket.pickupBlock(null, level, target, state);
    }
}
