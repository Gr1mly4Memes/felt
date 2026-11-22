package gr1mly4memes.felt.mixin.core.network;

import gr1mly4memes.felt.bridge.core.network.ConnectionLoginValidationBridge;
import gr1mly4memes.felt.mod.util.ConnectionLoginValidation;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionLoginValidationEventMixin implements ConnectionLoginValidationBridge {

    @Unique private final ConnectionLoginValidation.State felt$loginValidationState = new ConnectionLoginValidation.State();

    @Override
    public ConnectionLoginValidation.State bridge$getLoginValidationState() {
        return this.felt$loginValidationState;
    }
}
