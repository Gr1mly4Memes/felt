package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$6")
public abstract class TntDispenseBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "NEW",
        target = "(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/entity/item/PrimedTnt;"))
    private PrimedTnt felt$selectTnt(Level level, double x, double y, double z, LivingEntity actor, Operation<PrimedTnt> create,
                                    @Local(argsOnly = true) BlockSource source, @Local(argsOnly = true) ItemStack stack,
                                    @Share("tntSelection") LocalRef<DispenserBlockHooks.DispenseSelection> selected,
                                    @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        var selection = DispenserBlockHooks.selectDispensedItem(source, (DispenseItemBehavior) (Object) this, stack, new Vector(x, y, z));
        if (!selection.proceed()) {
            cir.setReturnValue(stack);
            return null;
        }
        selected.set(selection);
        var position = selection.event().getVelocity();
        return create.call(level, position.getX(), position.getY(), position.getZ(), actor);
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V"))
    private void felt$emitAtPrimedTnt(ServerLevel level, Entity entity, Holder<GameEvent> event, BlockPos originalPosition,
                                      Operation<Void> emit, @Share("tntSelection") LocalRef<DispenserBlockHooks.DispenseSelection> selected) {
        emit.call(level, entity, event, CraftVector.toBlockPos(selected.get().event().getVelocity()));
    }

    @WrapWithCondition(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private boolean felt$consumeTnt(ItemStack stack, int amount,
                                    @Share("tntSelection") LocalRef<DispenserBlockHooks.DispenseSelection> selected) {
        return selected.get().consumeOriginal();
    }
}
