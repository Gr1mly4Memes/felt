package gr1mly4memes.felt.mixin.core.world.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

/** Reuses the native listener and remote-state algorithms for selected inventory slots. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuSlotBroadcastAccessor {

    @Invoker("triggerSlotListeners")
    void felt$triggerSlotListeners(int rawSlot, ItemStack stack, Supplier<ItemStack> copy);

    @Invoker("synchronizeSlotToRemote")
    void felt$synchronizeSlotToRemote(int rawSlot, ItemStack stack, Supplier<ItemStack> copy);
}
