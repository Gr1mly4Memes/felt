package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.inventory.PosContainerBridge;
import gr1mly4memes.felt.mixin.core.world.SimpleContainerMixin;
import net.minecraft.world.inventory.LoomMenu;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/inventory/LoomMenu$2")
public abstract class LoomContainer2Mixin extends SimpleContainerMixin {
    @Shadow(aliases = {"this$0", "f_39905_", "field_17324"}, remap = false)
    private LoomMenu outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}