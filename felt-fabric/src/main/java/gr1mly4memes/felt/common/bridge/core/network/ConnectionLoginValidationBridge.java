package gr1mly4memes.felt.common.bridge.core.network;

import gr1mly4memes.felt.common.mod.util.ConnectionLoginValidation;

public interface ConnectionLoginValidationBridge {

    ConnectionLoginValidation.State bridge$getLoginValidationState();
}
