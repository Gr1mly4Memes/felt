package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.dispenser.MinecartDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BoatDispenseItemBehavior.class, MinecartDispenseItemBehavior.class})
public abstract class VehicleDispenseItemBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean felt$keepItemOnFailedSpawn(ServerLevel level, Entity entity, Operation<Boolean> original,
                                             @Share(value = "keepStack", namespace = "felt:dispense-vehicle") LocalBooleanRef keepStack) {
        boolean added = original.call(level, entity);
        if (!added) {
            keepStack.set(true);
        }
        return added;
    }

    @WrapWithCondition(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private boolean felt$consumeVehicleItem(ItemStack stack, int count,
                                            @Share(value = "keepStack", namespace = "felt:dispense-vehicle") LocalBooleanRef keepStack) {
        return !keepStack.get();
    }
}
