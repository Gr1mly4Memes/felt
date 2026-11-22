package gr1mly4memes.felt.mixin.core.world.inventory;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.bridge.core.world.inventory.PrepareResultMenuBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuPrepareResultMixin {

    @Shadow @Final private Level level;

    @WrapMethod(method = "slotsChanged(Lnet/minecraft/world/Container;)V", require = 1, allow = 1)
    private void felt$prepareSmithingResult(Container changed, Operation<Void> original) {
        PrepareResultMenuBridge bridge = (PrepareResultMenuBridge) this;
        boolean previouslyDeferred = bridge.bridge$isPrepareResultDeferred();
        bridge.bridge$setPrepareResultDeferred(true);
        try {
            original.call(changed);
            if (this.level instanceof ServerLevel && !previouslyDeferred) {
                // Parent createResult and hasRecipeError are settled. Emit once,
                // not once per native write plus once again in each menu layer.
                CraftEventFactory.callPrepareResultEvent((SmithingMenu) (Object) this, SmithingMenu.RESULT_SLOT);
            }
        } finally {
            bridge.bridge$setPrepareResultDeferred(previouslyDeferred);
        }
    }
}
