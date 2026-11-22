package gr1mly4memes.felt.mod.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GlobalConfigManager {

    private static final Logger LOGGER = LogManager.getLogger(GlobalConfigManager.class);
    private static final Map<String, List<String>> COMMENTS = new LinkedHashMap<>();

    private GlobalConfigManager() {
    }

    public static Map<String, List<String>> comments() {
        return COMMENTS;
    }

    public static void init() {
        for (Field field : FeltConfig.class.getDeclaredFields()) {
            GlobalConfig globalConfig = field.getAnnotation(GlobalConfig.class);
            if (globalConfig == null || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            String path = path(globalConfig);
            if (globalConfig.comment().length > 0) {
                COMMENTS.put(path, List.of(globalConfig.comment()));
            }
            try {
                loadField(field, globalConfig, path);
            } catch (Exception e) {
                LOGGER.warn("Failed to load config value '{}', using default", path, e);
                try {
                    FeltConfig.config.set(path, fileDefault(field));
                } catch (Exception ex) {
                    LOGGER.error("Failed to reset config value '{}'", path, ex);
                }
            }
        }
    }

    private static void loadField(Field field, GlobalConfig globalConfig, String path) throws Exception {
        ConfigVerify verify = instantiate(globalConfig.verify());
        boolean isEnumConfig = verify instanceof ConfigVerify.EnumConfigVerify;
        if (!FeltConfig.config.contains(path)) {
            FeltConfig.config.set(path, fileDefault(field));
        }
        Object savedValue = FeltConfig.config.get(path);
        if (isEnumConfig) {
            savedValue = verify.convert(savedValue.toString());
        } else if (savedValue != null && field.getType() == String.class && !(savedValue instanceof String)) {
            savedValue = String.valueOf(savedValue);
        }
        String checkInfo = verify.check(null, savedValue);
        if (checkInfo != null) {
            throw new IllegalArgumentException(checkInfo);
        }
        field.set(null, savedValue);
    }

    private static Object fileDefault(Field field) throws Exception {
        Object value = field.get(null);
        return value instanceof Enum<?> e ? e.name() : value;
    }

    private static String path(GlobalConfig config) {
        StringBuilder path = new StringBuilder("settings.");
        for (String category : config.category()) {
            path.append(category).append('.');
        }
        return path.append(config.name()).toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ConfigVerify instantiate(Class<? extends ConfigVerify<?>> type) throws Exception {
        Constructor<? extends ConfigVerify<?>> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (ConfigVerify) constructor.newInstance();
    }
}
