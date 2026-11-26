package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoatDispenseItemBehavior.class)
public abstract class BoatDispenseItemBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
    private Entity felt$selectDispensedBoat(EntityType<?> type, Level level, EntitySpawnReason reason, Operation<Entity> create,
                                           @Local(argsOnly = true) BlockSource source, @Local(argsOnly = true) ItemStack stack,
                                           @Local(index = 8) double x, @Local(index = 10) double y, @Local(index = 12) double z,
                                           @Local(index = 15) double yOffset, @Share("position") LocalRef<Vector> position,
                                           @Share(value = "keepStack", namespace = "felt:dispense-vehicle") LocalBooleanRef keepStack,
                                           @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        var selection = DispenserBlockHooks.selectDispensedItem(source, (BoatDispenseItemBehavior) (Object) this, stack,
            new Vector(x, y + yOffset, z));
        if (!selection.proceed()) {
            cir.setReturnValue(stack);
            return null;
        }
        position.set(selection.event().getVelocity());
        keepStack.set(!selection.consumeOriginal());
        return create.call(type, level, reason);
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;setInitialPos(DDD)V"))
    private void felt$setBoatPosition(AbstractBoat boat, double x, double y, double z, Operation<Void> original,
                                      @Share("position") LocalRef<Vector> position) {
        var selected = position.get();
        original.call(boat, selected.getX(), selected.getY(), selected.getZ());
    }
}
