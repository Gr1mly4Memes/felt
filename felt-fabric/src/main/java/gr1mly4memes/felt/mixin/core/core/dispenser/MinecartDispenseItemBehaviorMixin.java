package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.MinecartDispenseItemBehavior;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartDispenseItemBehavior.class)
public abstract class MinecartDispenseItemBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;createMinecart(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;"))
    private AbstractMinecart felt$selectDispensedMinecart(Level level, double x, double y, double z,
                                                         EntityType<? extends AbstractMinecart> type, EntitySpawnReason reason,
                                                         ItemStack stack, Player player, Operation<AbstractMinecart> create,
                                                         @Local(argsOnly = true) BlockSource source,
                                                         @Share(value = "keepStack", namespace = "felt:dispense-vehicle") LocalBooleanRef keepStack,
                                                         @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        var selection = DispenserBlockHooks.selectDispensedItem(source, (MinecartDispenseItemBehavior) (Object) this, stack,
            new Vector(x, y, z));
        if (!selection.proceed()) {
            cir.setReturnValue(stack);
            return null;
        }
        keepStack.set(!selection.consumeOriginal());
        var position = selection.event().getVelocity();
        return create.call(level, position.getX(), position.getY(), position.getZ(), type, reason,
            CraftItemStack.asNMSCopy(selection.event().getItem()), player);
    }
}
