package gr1mly4memes.felt.mixin.vanilla.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin_Vanilla {

    // Wrap the transaction's entry, not burn's second list access (which is fuel
    // after output has already been written). The successor credit uses this same
    // invocation-local result, so cancellation cannot add recipe/experience credit.
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void felt$furnaceSmelt(NonNullList<ItemStack> items, ItemStack input, ItemStack result, Operation<Void> original,
                                             @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace,
                                             @Local(ordinal = 0) net.minecraft.world.item.crafting.RecipeHolder<?> recipe,
                                             @Share(value = "smelted", namespace = "felt:furnace") LocalBooleanRef smelted) {
        smelted.set(false);
        ItemStack replacement = FeltEventFactory.smeltResult((net.minecraft.server.level.ServerLevel) furnace.getLevel(),
            furnace.getBlockPos(), items, input, result, recipe);
        if (replacement == null) {
            return;
        }
        // Native burn retains component-aware output, wet-sponge and input handling;
        // previous/next third-party wrappers still receive the accepted event result.
        original.call(items, input, replacement);
        smelted.set(true);
    }

    @gr1mly4memes.felttools.mixin.Decorate(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;consumeFuel(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void felt$consumeFuel(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                                             NonNullList<ItemStack> items, ItemStack fuel,
                                             @gr1mly4memes.felttools.mixin.Local(allocate = "feltFurnaceBurn") org.bukkit.event.inventory.FurnaceBurnEvent event) throws Throwable {
        if (event.willConsumeFuel()) {
            gr1mly4memes.felttools.mixin.DecorationOps.callsite().invoke(level, pos, items, fuel);
        }
    }
}
