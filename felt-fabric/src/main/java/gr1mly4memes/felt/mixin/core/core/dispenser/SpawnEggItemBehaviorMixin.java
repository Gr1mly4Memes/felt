package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.SpawnEggItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnEggItemBehavior.class)
public abstract class SpawnEggItemBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/SpawnEggItem;getType(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/EntityType;"))
    private EntityType<?> felt$selectSpawnEgg(ItemStack stack, Operation<EntityType<?>> lookup,
                                              @Local(argsOnly = true) BlockSource source,
                                              @Share("spawnEgg") LocalRef<ItemStack> selected,
                                              @Share("consumeEgg") LocalBooleanRef consume,
                                              @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        EntityType<?> type = lookup.call(stack);
        if (type == null) {
            return null;
        }
        var selection = DispenserBlockHooks.selectDispensedItem(source, (SpawnEggItemBehavior) (Object) this, stack, new Vector());
        if (!selection.proceed()) {
            cir.setReturnValue(stack);
            return null;
        }
        ItemStack egg = CraftItemStack.asNMSCopy(selection.event().getItem());
        selected.set(egg);
        consume.set(selection.consumeOriginal());
        return consume.get() ? type : lookup.call(egg);
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"))
    private Entity felt$spawnSelectedEntity(EntityType<?> type, ServerLevel level, ItemStack stack, LivingEntity actor,
                                            BlockPos pos, EntitySpawnReason reason, boolean offset, boolean deepOffset,
                                            Operation<Entity> spawn,
                                            @Share("spawnEgg") LocalRef<ItemStack> selected) {
        return spawn.call(type, level, selected.get(), actor, pos, reason, offset, deepOffset);
    }

    @WrapWithCondition(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private boolean felt$consumeSpawnEgg(ItemStack stack, int amount,
                                         @Share("consumeEgg") LocalBooleanRef consume) {
        return consume.get();
    }
}
