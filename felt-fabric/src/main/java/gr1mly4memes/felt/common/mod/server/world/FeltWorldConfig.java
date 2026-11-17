package gr1mly4memes.felt.common.mod.server.world;

import gr1mly4memes.felt.common.mixin.core.world.level.LevelMixin;
import org.spigotmc.SpigotWorldConfig;

public class FeltWorldConfig {

    /**
     * Use as a marker world name. We don't want to put trash output in terminal
     * only for reading a default world config.
     * @see gr1mly4memes.felt.common.mixin.bukkit.SpigotWorldConfigMixin#arclight$skipLog(String)
     */
    @SuppressWarnings({"StringOperationCanBeSimplified", "JavadocReference"})
    public static final String DEFAULT_MARKER = new String("default");

    /**
     * Default world config. Used for logic world.
     * @see LevelMixin#bridge$spigotConfig()
     */
    public static final SpigotWorldConfig DEFAULT = SpigotWorldConfig.forDefaults(DEFAULT_MARKER);

    private FeltWorldConfig() {}
}
