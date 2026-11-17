package gr1mly4memes.felt.common.mod;

import gr1mly4memes.felttools.api.FeltPlatform;
import gr1mly4memes.felt.common.mod.util.log.FeltLogger;
import gr1mly4memes.felttools.mixin.MixinTools;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class FeltConnector implements IMixinConnector {

    public static final Logger LOGGER = FeltLogger.getLogger("Felt");

    @Override
    public void connect() {
        MixinTools.setup();
        Mixins.addConfiguration("mixins.felt.core.json");
        Mixins.addConfiguration("mixins.felt.bukkit.json");
        Mixins.addConfiguration("mixins.felt.compat.json");
        switch (FeltPlatform.current()) {
            case VANILLA -> Mixins.addConfiguration("mixins.felt.vanilla.json");
        }
        LOGGER.info("Felt core mixin added.");
        Mixins.addConfiguration("mixins.felt.impl.optimization.json");
        LOGGER.info("Felt optimization mixin added.");
    }
}
