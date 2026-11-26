package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import gr1mly4memes.felt.mod.server.event.FeltShearing;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsDispenseItemBehavior.class)
public abstract class ShearsDispenseItemBehaviorMixin extends OptionalDispenseItemBehavior {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void felt$dispenseShears(BlockSource source, ItemStack tool, CallbackInfoReturnable<ItemStack> cir) {
        var original = CraftItemStack.asBukkitMirror(tool);
        var event = new BlockDispenseEvent(CraftBlock.at(source.level(), source.pos()), original.clone(), new Vector());
        if (!event.callEvent()) {
            this.setSuccess(false);
            cir.setReturnValue(tool);
        } else if (!original.equals(event.getItem())) {
            ItemStack replacement = CraftItemStack.asNMSCopy(event.getItem());
            var behavior = ((DispenserBlock) source.state().getBlock()).getDispenseMethod(source.level(), replacement);
            if (behavior != DispenseItemBehavior.NOOP && behavior != this) {
                behavior.dispense(source, replacement);
                cir.setReturnValue(tool);
            }
        }
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/dispenser/ShearsDispenseItemBehavior;tryShearEntity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean felt$withShearsSource(ServerLevel level, BlockPos target, ItemStack tool, Operation<Boolean> original,
                                         @Local(argsOnly = true) BlockSource source) {
        return DispenserBlockHooks.withSource(source, this, () -> original.call(level, target, tool));
    }

    @WrapOperation(method = "tryShearEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Shearable;readyForShearing()Z"))
    private static boolean felt$canShear(Shearable shearable, Operation<Boolean> original,
                                         @Local(argsOnly = true) ItemStack tool,
                                         @Share("shearedDrops") LocalRef<List<ItemStack>> selectedDrops) {
        if (!original.call(shearable)) {
            return false;
        }
        var call = DispenserBlockHooks.currentDispense();
        if (call == null) {
            return true;
        }
        BlockSource source = call.source();
        Entity entity = (Entity) shearable;
        var event = CraftEventFactory.callBlockShearEntityEvent(entity, CraftBlock.at(source.level(), source.pos()),
            CraftItemStack.asBukkitMirror(tool), FeltShearing.defaultDrops(entity, source.level(), tool));
        if (event.isCancelled()) {
            return false;
        }
        selectedDrops.set(CraftItemStack.asNMSCopy(event.getDrops()));
        return true;
    }

    @WrapOperation(method = "tryShearEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Shearable;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void felt$shearWithSelectedDrops(Shearable shearable, ServerLevel level, SoundSource sound, ItemStack tool,
                                                   Operation<Void> original, @Share("shearedDrops") LocalRef<List<ItemStack>> selectedDrops) {
        List<ItemStack> drops = selectedDrops.get();
        if (drops == null) {
            original.call(shearable, level, sound, tool);
            return;
        }
        var previous = FeltShearing.captureDrops((Entity) shearable, tool, drops);
        try {
            original.call(shearable, level, sound, tool);
        } finally {
            FeltShearing.restoreDrops(previous);
        }
    }
}
