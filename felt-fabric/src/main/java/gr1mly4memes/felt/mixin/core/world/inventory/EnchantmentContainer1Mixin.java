package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.inventory.PosContainerBridge;
import gr1mly4memes.felt.mixin.core.world.SimpleContainerMixin;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/inventory/EnchantmentMenu$1")
public abstract class EnchantmentContainer1Mixin extends SimpleContainerMixin {
    @Shadow(aliases = {"this$0", "f_39494_", "field_7815"}, remap = false)
    private EnchantmentMenu outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}