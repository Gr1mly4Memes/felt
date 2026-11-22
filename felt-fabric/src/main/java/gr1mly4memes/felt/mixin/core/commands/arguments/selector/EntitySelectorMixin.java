package gr1mly4memes.felt.mixin.core.commands.arguments.selector;

import gr1mly4memes.felt.bridge.core.commands.CommandSourceStackBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin {

    @Decorate(method = "checkPermissions(Lnet/minecraft/commands/CommandSourceStack;)V", inject = true, at = @At("HEAD"))
    private void felt$bypassSelectorPermissions(CommandSourceStack sender) throws Throwable {
        if (((CommandSourceStackBridge) sender).bridge$isBypassSelectorPermissions()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
