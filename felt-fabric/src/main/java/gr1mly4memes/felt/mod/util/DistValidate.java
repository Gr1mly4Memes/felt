package gr1mly4memes.felt.mod.util;

import gr1mly4memes.felt.mod.server.FeltServer;
import gr1mly4memes.felt.mod.config.FeltConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DistValidate {

    private static final Marker MARKER = MarkerManager.getMarker("EXT_LOGIC");

    public static boolean isValid(UseOnContext context) {
        return context != null && isValid(context.getLevel());
    }

    public static boolean isValid(LevelAccessor level) {
        return level != null
            && !level.isClientSide()
            && isLogicWorld(level);
    }

    public static boolean isValid(BlockGetter getter) {
        return getter instanceof LevelAccessor level && isValid(level);
    }

    private static boolean isLogicWorld(LevelAccessor level) {
        var cl = level.getClass();
        return cl == ServerLevel.class || cl == WorldGenRegion.class
            || isLogicWorld(cl);
    }

    private static final Map<Class<?>, Boolean> SEEN_CLASSES = new ConcurrentHashMap<>();

    private static boolean isLogicWorld(Class<?> cl) {
        return SEEN_CLASSES.computeIfAbsent(cl, c -> {
            var name = c.getName();
            var result = FeltConfig.extraLogicWorlds.contains(cl.getName());
            FeltServer.LOGGER.warn(MARKER, "Level class {} treated as logic world: {}", name, result);
            return result;
        });
    }
}
