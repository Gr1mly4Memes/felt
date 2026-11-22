package gr1mly4memes.felt.bridge.core.network;

import gr1mly4memes.felt.mod.util.ConnectionLoginValidation;

public interface ConnectionLoginValidationBridge {

    ConnectionLoginValidation.State bridge$getLoginValidationState();
}
