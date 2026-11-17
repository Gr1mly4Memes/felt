package gr1mly4memes.felt.common.mixin.core.world.inventory;

import gr1mly4memes.felt.common.bridge.core.world.inventory.SlotBridge;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Slot.class)
public abstract class SlotMixin implements SlotBridge {

    // @formatter:off
    @Shadow protected abstract void onSwapCraft(int numItemsCrafted);
    // @formatter:on

    @Override
    public void bridge$onSwapCraft(int numItemsCrafted) {
        onSwapCraft(numItemsCrafted);
    }
}
