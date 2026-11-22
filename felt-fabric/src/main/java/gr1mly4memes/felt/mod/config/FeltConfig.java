package gr1mly4memes.felt.mod.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class FeltConfig {

    public static final List<String> CONFIG_HEADER = List.of(
            "This is the main configuration file for Felt.",
            "",
            "Created by Gr1mly4Memes"
    );
    public static final int CURRENT_CONFIG_VERSION = 2;

    private static File configFile;
    public static YamlConfiguration config;
    private static int configVersion;

    private static final Logger LOGGER = LogManager.getLogger(FeltConfig.class);

    // optimization
    @GlobalConfig(name = "cache-plugin-class", category = {"optimization"},
        comment = {"Cache plugin class lookups to improve performance."})
    public static boolean cachePluginClass = true;

    @GlobalConfig(name = "goal-selector-update-interval", category = {"optimization"},
        comment = {
            "Interval in ticks to update the goal selector.",
            "Higher values cost less resources but cause mobs to change their goals less often."
        })
    public static int goalSelectorUpdateInterval = 2;

    @GlobalConfig(name = "use-activation-and-tracking-range", category = {"optimization"},
        comment = {"Whether to use Spigot tracking range optimization."})
    public static boolean useActivationAndTrackingRange = true;

    // async-catcher
    @GlobalConfig(name = "default-operation", category = {"async-catcher"}, verify = AsyncCatcherOperationVerify.class,
        comment = {
            "Default async catcher operation.",
            "NONE - Continue on the current thread",
            "DISPATCH - Dispatch to the main thread without blocking",
            "BLOCK - Run in main thread and wait for results",
            "EXCEPTION - Raise an error"
        })
    public static AsyncCatcherOperation asyncCatcherDefaultOperation = AsyncCatcherOperation.BLOCK;

    @GlobalConfig(name = "dump", category = {"async-catcher"},
        comment = {"Dump stack trace information in debug.log."})
    public static boolean asyncCatcherDump = true;

    @GlobalConfig(name = "warn", category = {"async-catcher"},
        comment = {"Warn when the async catcher blocks the main thread."})
    public static boolean asyncCatcherWarn = true;

    // compatibility
    @GlobalConfig(name = "symlink-world", category = {"compatibility"},
        comment = {
            "Create symbol links to mod dimension folders that match Bukkit world names.",
            "Enabling this can improve plugin compatibility.",
            "Changing this on a production server will change mod world names and may cause",
            "data loss for plugins relying on world names."
        })
    public static boolean symlinkWorld = false;

    @GlobalConfig(name = "forward-permission", category = {"compatibility"},
        comment = {
            "true - Forward ModLoader permission queries to Bukkit",
            "false - Disable permission forward",
            "reverse - Forward Bukkit player permission queries to ModLoader"
        })
    public static String forwardPermission = "true";

    @GlobalConfig(name = "valid-username-regex", category = {"compatibility"},
        comment = {
            "Regex for valid username check. Leave blank to fall back to the vanilla check.",
            "Following allows Chinese characters:",
            "valid-username-regex = \"^[ -~\\\\p{sc=Han}]{1,16}$\"",
            "Following allows any username to login:",
            "valid-username-regex = \".+\""
        })
    public static String validUsernameRegex = "";

    @GlobalConfig(name = "isolate-adventure-from-modloader", category = {"compatibility"},
        comment = {
            "Prevent plugins from loading Kyori Adventure from the ModLoader.",
            "This is only a compatibility option and persists for now."
        })
    public static boolean isolateAdventureFromModloader = false;

    // experimental
    @GlobalConfig(name = "replaceable-base-world-generator", category = {"experimental"},
        comment = {
            "Allows replacing the base world generator for Bukkit created worlds.",
            "This provides compatibility with TerraBlender and more options for Bukkit's multi world support.",
            "It is not recommended to create dimensions specific to a mod.",
            "Setting this for an already generated world may lead to tearing."
        })
    public static boolean replaceableBaseWorldGenerator = false;

    // complex values loaded manually
    public static Map<String, AsyncCatcherOperation> asyncCatcherOverrides = new LinkedHashMap<>();
    public static List<String> extraLogicWorlds = new ArrayList<>();
    public static List<String> isolatePluginClassLoaders = new ArrayList<>();
    public static Map<String, MaterialPropertySpec> materialPropertyOverrides = new LinkedHashMap<>();
    public static Map<String, EntityPropertySpec> entityPropertyOverrides = new LinkedHashMap<>();

    private static final Map<String, List<String>> COMPLEX_COMMENTS = Map.of(
        "settings.async-catcher.overrides", List.of(
            "Per-operation overrides for the async catcher.",
            "Keys are class name prefixes, values are NONE, DISPATCH, BLOCK or EXCEPTION."
        ),
        "settings.compatibility.extra-logic-worlds", List.of(
            "Extra worlds running logic.",
            "If any mods do not function well, try searching class names in logs related to [EXT_LOGIC] and add them here."
        ),
        "settings.compatibility.isolate-plugin-class-loaders", List.of(
            "An illegal access means that a plugin loads a class from another plugin that is not a depend or soft-depend of itself.",
            "When a package name is given, no illegal access will be attempted on classes prefixed with the package name.",
            "This is especially useful when class loading issues happen.",
            "Beware that it may break plugins relying on illegal access."
        ),
        "settings.compatibility.material-property-overrides", List.of(
            "Material property overrides for Bukkit compatibility."
        ),
        "settings.compatibility.entity-property-overrides", List.of(
            "Entity property overrides for Bukkit compatibility."
        )
    );

    private FeltConfig() {
    }

    public static void init(File file) {
        configFile = file;
        config = new YamlConfiguration();
        config.options().setHeader(CONFIG_HEADER);
        config.options().copyDefaults(true);
        try {
            if (!file.exists() && !file.createNewFile()) {
                throw new IllegalStateException("Can't create file");
            }
            config.load(file);
        } catch (Exception ex) {
            LOGGER.error("Failure to load felt config", ex);
            throw new RuntimeException(ex);
        }
        configVersion = config.getInt("config-version", CURRENT_CONFIG_VERSION);
        config.set("config-version", CURRENT_CONFIG_VERSION);
        GlobalConfigManager.init();
        loadComplex();
        save();
    }

    public static void save() {
        try {
            config.save(configFile);
            writeComments();
        } catch (Exception ex) {
            LOGGER.error("Unable to save felt config", ex);
        }
    }

    private static void writeComments() {
        Map<String, List<String>> comments = new LinkedHashMap<>(GlobalConfigManager.comments());
        comments.putAll(COMPLEX_COMMENTS);
        if (comments.isEmpty()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            List<String> result = new ArrayList<>(lines.size() + 64);
            List<String> pathKeys = new ArrayList<>();
            List<Integer> pathIndents = new ArrayList<>();
            for (String line : lines) {
                String key = yamlKey(line);
                if (key != null) {
                    int indent = line.length() - line.stripLeading().length();
                    while (!pathIndents.isEmpty() && pathIndents.get(pathIndents.size() - 1) >= indent) {
                        pathIndents.remove(pathIndents.size() - 1);
                        pathKeys.remove(pathKeys.size() - 1);
                    }
                    String path = pathKeys.isEmpty() ? key : String.join(".", pathKeys) + "." + key;
                    List<String> comment = comments.get(path);
                    if (comment != null) {
                        String prefix = " ".repeat(indent) + "# ";
                        for (String c : comment) {
                            result.add(prefix + c);
                        }
                    }
                    if (isSection(line)) {
                        pathKeys.add(key);
                        pathIndents.add(indent);
                    }
                }
                result.add(line);
            }
            Files.write(configFile.toPath(), result, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOGGER.warn("Unable to write config comments", ex);
        }
    }

    private static String yamlKey(String line) {
        String trimmed = line.stripLeading();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) {
            return null;
        }
        int colon = trimmed.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        String key = trimmed.substring(0, colon).trim();
        if (key.isEmpty() || key.startsWith("\"") || key.startsWith("'")) {
            return null;
        }
        for (char c : key.toCharArray()) {
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.')) {
                return null;
            }
        }
        return key;
    }

    private static boolean isSection(String line) {
        String trimmed = line.stripLeading();
        int colon = trimmed.indexOf(':');
        return colon >= 0 && trimmed.substring(colon + 1).isBlank();
    }

    public static int getConfigVersion() {
        return configVersion;
    }

    public static boolean isForwardPermission() {
        return "true".equals(forwardPermission);
    }

    public static boolean isForwardPermissionReverse() {
        return "reverse".equals(forwardPermission);
    }

    public static boolean isIsolatedPluginClassLoaders(String name) {
        for (String prefix : isolatePluginClassLoaders) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<MaterialPropertySpec> getMaterialSpec(String key) {
        return Optional.ofNullable(materialPropertyOverrides.get(key));
    }

    public static Optional<EntityPropertySpec> getEntitySpec(String key) {
        return Optional.ofNullable(entityPropertyOverrides.get(key));
    }

    public static Map<String, String> asyncCatcherOverridesAsStrings() {
        Map<String, String> ret = new LinkedHashMap<>();
        asyncCatcherOverrides.forEach((key, value) -> ret.put(key, value.name()));
        return ret;
    }

    private static void loadComplex() {
        extraLogicWorlds = readStringList("settings.compatibility.extra-logic-worlds");
        isolatePluginClassLoaders = readStringList("settings.compatibility.isolate-plugin-class-loaders");
        asyncCatcherOverrides = readAsyncCatcherOverrides();
        materialPropertyOverrides = readMaterialPropertyOverrides();
        entityPropertyOverrides = readEntityPropertyOverrides();
    }

    private static List<String> readStringList(String path) {
        if (!config.contains(path)) {
            config.set(path, List.of());
        }
        List<String> value = config.getStringList(path);
        return value == null ? new ArrayList<>() : new ArrayList<>(value);
    }

    private static Map<String, AsyncCatcherOperation> readAsyncCatcherOverrides() {
        String path = "settings.async-catcher.overrides";
        Map<String, AsyncCatcherOperation> ret = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            config.createSection(path);
            return ret;
        }
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value == null) {
                continue;
            }
            try {
                ret.put(key, AsyncCatcherOperation.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid async-catcher override operation '{}' for '{}'", value, key);
            }
        }
        return ret;
    }

    private static Map<String, MaterialPropertySpec> readMaterialPropertyOverrides() {
        String path = "settings.compatibility.material-property-overrides";
        Map<String, MaterialPropertySpec> ret = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            config.createSection(path);
            return ret;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) {
                continue;
            }
            MaterialPropertySpec spec = new MaterialPropertySpec();
            spec.materialDataClass = getString(child, "materialDataClass");
            spec.maxStack = getInteger(child, "maxStack");
            spec.maxDurability = getInteger(child, "maxDurability");
            spec.edible = getBoolean(child, "edible");
            spec.record = getBoolean(child, "record");
            spec.solid = getBoolean(child, "solid");
            spec.air = getBoolean(child, "air");
            spec.transparent = getBoolean(child, "transparent");
            spec.flammable = getBoolean(child, "flammable");
            spec.burnable = getBoolean(child, "burnable");
            spec.fuel = getBoolean(child, "fuel");
            spec.occluding = getBoolean(child, "occluding");
            spec.gravity = getBoolean(child, "gravity");
            spec.interactable = getBoolean(child, "interactable");
            spec.hardness = getFloat(child, "hardness");
            spec.blastResistance = getFloat(child, "blastResistance");
            spec.craftingRemainingItem = getString(child, "craftingRemainingItem");
            spec.itemMetaType = getString(child, "itemMetaType");
            spec.blockStateClass = getString(child, "blockStateClass");
            ret.put(key, spec);
        }
        return ret;
    }

    private static Map<String, EntityPropertySpec> readEntityPropertyOverrides() {
        String path = "settings.compatibility.entity-property-overrides";
        Map<String, EntityPropertySpec> ret = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            config.createSection(path);
            return ret;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) {
                continue;
            }
            EntityPropertySpec spec = new EntityPropertySpec();
            spec.entityClass = getString(child, "entityClass");
            spec.entityImplClass = getString(child, "entityImplClass");
            ret.put(key, spec);
        }
        return ret;
    }

    private static String getString(ConfigurationSection section, String key) {
        Object value = section.get(key);
        return value instanceof String s ? s : null;
    }

    private static Integer getInteger(ConfigurationSection section, String key) {
        Object value = section.get(key);
        return value instanceof Number n ? n.intValue() : null;
    }

    private static Float getFloat(ConfigurationSection section, String key) {
        Object value = section.get(key);
        return value instanceof Number n ? n.floatValue() : null;
    }

    private static Boolean getBoolean(ConfigurationSection section, String key) {
        Object value = section.get(key);
        return value instanceof Boolean b ? b : null;
    }

    public static class AsyncCatcherOperationVerify extends ConfigVerify.EnumConfigVerify<AsyncCatcherOperation> {
    }
}
