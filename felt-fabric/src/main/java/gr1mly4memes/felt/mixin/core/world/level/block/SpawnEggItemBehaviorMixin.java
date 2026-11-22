package gr1mly4memes.felt.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.SpawnEggItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpawnEggItemBehavior.class)
public abstract class SpawnEggItemBehaviorMixin {

    // The parent dispenser flag is reset before dispense; this guard also handles nested plugin
    // callbacks and is restored even when a listener or replacement behavior throws.
    private static Boolean felt$eventInProgress = false;

    // Native execute catches Exception around EntityType.spawn. Wrap the method instead of
    // decorating that invocation so listener/replacement exceptions escape like fixed CB;
    // original.call still owns the complete native spawn, Neo hook, catch, shrink and game event.
    // Changed NOOP/self keeps the refunded count while retaining native spawn/failure handling.
    @WrapMethod(method = "execute")
    private ItemStack felt$dispenseEgg(BlockSource source, ItemStack dispensed, Operation<ItemStack> original) {
        // Native returns unchanged before the spawn try when the egg has no entity type.
        if (SpawnEggItem.getType(dispensed) == null) {
            return original.call(source, dispensed);
        }

        ItemStack single = dispensed.split(1);
        boolean refunded = false;
        boolean cancel = false;
        boolean preserveCount = false;
        boolean nested = felt$eventInProgress;
        felt$eventInProgress = true;
        try {
            CraftItemStack craftItem = CraftItemStack.asCraftMirror(single);
            BlockDispenseEvent event = new BlockDispenseEvent(CraftBlock.at(source.level(), source.pos()), craftItem.clone(), new Vector(0, 0, 0));
            if (!DispenserBlockHooks.isEventFired() && !nested) {
                org.bukkit.Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                dispensed.grow(1);
                refunded = true;
                cancel = true;
            } else if (!event.getItem().equals(craftItem)) {
                // CraftBukkit restores before converting the replacement and registry lookup.
                // In particular, a nested behavior must see the original stack restored.
                dispensed.grow(1);
                refunded = true;
                ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                DispenseItemBehavior behavior = ((DispenserBlock) source.state().getBlock())
                    .getDispenseMethod(source.level(), eventStack);
                if (behavior != DispenseItemBehavior.NOOP && behavior != (Object) this) {
                    behavior.dispense(source, eventStack);
                    cancel = true;
                } else {
                    preserveCount = true;
                }
            }
        } finally {
            try {
                if (!refunded) {
                    dispensed.grow(1);
                }
            } finally {
                // A refund hook may throw; always restore the enclosing event guard.
                felt$eventInProgress = nested;
            }
        }
        if (cancel) {
            return dispensed;
        }
        // Keep native null-spawn, caught-failure and game-event behavior; suppress only shrink.
        ItemStack previous = felt$preservedStack;
        felt$preservedStack = preserveCount ? dispensed : null;
        try {
            return original.call(source, dispensed);
        } finally {
            felt$preservedStack = previous;
        }
    }

    private static ItemStack felt$preservedStack;

    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"), require = 1)
    private void felt$keepRefund(ItemStack stack, int amount, Operation<Void> original) {
        if (felt$preservedStack != stack) {
            original.call(stack, amount);
        }
    }
}
