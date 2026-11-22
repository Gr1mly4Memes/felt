package gr1mly4memes.felt.mixin.core.commands.arguments.selector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gr1mly4memes.felt.bridge.core.commands.arguments.selector.EntitySelectorParserBridge;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntitySelectorParser.class)
public abstract class EntitySelectorParserMixin implements EntitySelectorParserBridge {

    // @formatter:off
    @Shadow private boolean usesSelectors;
    @Shadow protected abstract void parseSelector() throws CommandSyntaxException;
    @Shadow public abstract EntitySelector parse() throws CommandSyntaxException;
    // @formatter:on

    private Boolean felt$overridePermissions;

    @Override
    public EntitySelector bridge$parse(boolean overridePermissions) throws CommandSyntaxException {
        return this.parse(overridePermissions);
    }

    public EntitySelector parse(boolean overridePermissions) throws CommandSyntaxException {
        Boolean previous = this.felt$overridePermissions;
        try {
            this.felt$overridePermissions = overridePermissions;
            return this.parse();
        } finally {
            this.felt$overridePermissions = previous;
        }
    }

    @Override
    public void bridge$parseSelector(boolean overridePermissions) throws CommandSyntaxException {
        this.parseSelector(overridePermissions);
    }

    public void parseSelector(boolean overridePermissions) throws CommandSyntaxException {
        Boolean previous = this.felt$overridePermissions;
        try {
            this.felt$overridePermissions = overridePermissions;
            this.parseSelector();
        } finally {
            this.felt$overridePermissions = previous;
        }
    }

    @Inject(method = "parseSelector()V", at = @At(value = "FIELD",
        target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;usesSelectors:Z",
        opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    public void felt$onParserSelector(CallbackInfo ci) {
        if (this.felt$overridePermissions != null) {
            this.usesSelectors = !this.felt$overridePermissions;
        }
    }
}
