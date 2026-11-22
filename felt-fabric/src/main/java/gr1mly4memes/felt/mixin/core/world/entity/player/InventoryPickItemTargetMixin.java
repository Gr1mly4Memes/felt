package gr1mly4memes.felt.mixin.core.world.entity.player;

import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Inventory.class)
public abstract class InventoryPickItemTargetMixin {

    @Decorate(method = {"pickSlot(I)V", "addAndPickItem(Lnet/minecraft/world/item/ItemStack;)V"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSuitableHotbarSlot()I"),
        require = 2, allow = 2)
    private int felt$pickItemTarget(Inventory inventory) throws Throwable {
        int nativeSlot = (int) DecorationOps.callsite().invoke(inventory);
        var target = FeltCaptures.pickItemTarget;
        return target != null && target.getFirst() == inventory && target.getSecond() >= 0 ? target.getSecond() : nativeSlot;
    }
}
