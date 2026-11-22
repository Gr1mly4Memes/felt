package gr1mly4memes.felt.mixin.core.world.entity.animal;

import gr1mly4memes.felt.bridge.core.world.entity.animal.FoxBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityToggleSitEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Fox.class)
public abstract class FoxMixin extends AnimalMixin implements FoxBridge {

    // @formatter:off
    @Invoker("addTrustedEntity") @Override public abstract void bridge$addTrustedEntity(LivingEntity entity);
    // @formatter:on

    @Unique private boolean felt$skipSitEvent;

    @Decorate(method = "setSitting(Z)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void felt$toggleSit(boolean value) throws Throwable {
        // Keep the load-time sitting bypass independent of the fox's sleeping state.
        boolean callEvent = !this.felt$skipSitEvent;
        this.felt$skipSitEvent = false;
        if (callEvent && !new EntityToggleSitEvent(this.getBukkitEntity(), value).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Decorate(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/fox/Fox;setSitting(Z)V"), require = 1, allow = 1)
    private void felt$restoreSitting(Fox fox, boolean value) throws Throwable {
        boolean previous = this.felt$skipSitEvent;
        this.felt$skipSitEvent = true;
        try {
            DecorationOps.callsite().invoke(fox, value);
        } finally {
            this.felt$skipSitEvent = previous;
        }
    }

    @WrapOperation(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/fox/Fox;canHoldItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean felt$pickupEvent(Fox foxEntity, ItemStack stack, Operation<Boolean> original,
            ServerLevel level, ItemEntity itemEntity, @Local(index = 3) LocalRef<ItemStack> itemStack) {
        boolean allowed = original.call(foxEntity, stack);
        var event = CraftEventFactory.callEntityPickupItemEvent(foxEntity, itemEntity, stack.getCount() - 1, !allowed);
        // The event may replace ItemEntity's stack; the native count/splits/take all read slot 3.
        itemStack.set(itemEntity.getItem());
        return !event.isCancelled();
    }

    @WrapOperation(method = "pickUpItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"), require = 1, expect = 1, allow = 1)
    private void felt$pickCause(ItemEntity itemEntity, Operation<Void> original) {
        itemEntity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
        try {
            original.call(itemEntity);
        } finally {
            // One-shot cleanup also covers a chained operation skipping discard or throwing.
            itemEntity.bridge().bridge$pushEntityRemoveCause(null);
        }
    }
}
