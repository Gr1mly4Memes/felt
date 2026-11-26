package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$1")
public abstract class ArmorStandDispenseBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityType;appendDefaultStackConfig(Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/entity/PostSpawnProcessor;"))
    private PostSpawnProcessor<ArmorStand> felt$selectStand(PostSpawnProcessor<ArmorStand> processor, Level level, ItemStack stack,
                                                            LivingEntity actor, Operation<PostSpawnProcessor<ArmorStand>> configure,
                                                            @Local(argsOnly = true) BlockSource source,
                                                            @Share("consumeStand") LocalBooleanRef consume,
                                                            @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        var selection = DispenserBlockHooks.selectDispensedItem(source, (DispenseItemBehavior) (Object) this, stack, new Vector());
        if (!selection.proceed()) {
            cir.setReturnValue(stack);
            return null;
        }
        consume.set(selection.consumeOriginal());
        return configure.call(processor, level, CraftItemStack.asNMSCopy(selection.event().getItem()), actor);
    }

    @WrapWithCondition(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private boolean felt$consumeStand(ItemStack stack, int amount, @Share("consumeStand") LocalBooleanRef consume) {
        return consume.get();
    }
}
