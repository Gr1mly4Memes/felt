package gr1mly4memes.felt.common.mixin.core.world.inventory;

import gr1mly4memes.felt.common.bridge.core.world.inventory.PosContainerBridge;
import gr1mly4memes.felt.common.mixin.core.world.SimpleContainerMixin;
import net.minecraft.world.inventory.LoomMenu;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/inventory/LoomMenu$1")
public abstract class LoomContainer1Mixin extends SimpleContainerMixin {
    @Shadow(aliases = {"this$0", "f_39900_", "field_7851"}, remap = false)
    private LoomMenu outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}