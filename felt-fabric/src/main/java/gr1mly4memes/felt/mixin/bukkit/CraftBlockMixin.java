package gr1mly4memes.felt.mixin.bukkit;

import gr1mly4memes.felt.bridge.bukkit.MaterialBridge;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftBlock.class, remap = false)
public abstract class CraftBlockMixin {

    // @formatter:off
    @Shadow public abstract Material getType();
    // @formatter:on

    @Inject(method = "getState", cancellable = true, at = @At("HEAD"))
    private void felt$getState(CallbackInfoReturnable<BlockState> cir) {
        MaterialBridge bridge = (MaterialBridge) (Object) getType();
        if (bridge.bridge$shouldApplyStateFactory()) {
            cir.setReturnValue(bridge.bridge$blockStateFactory().apply((CraftBlock) (Object) this));
        }
    }
}
