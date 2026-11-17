package gr1mly4memes.felt.common.mod.util.remapper.patcher;

import gr1mly4memes.felttools.api.PluginPatcher;
import gr1mly4memes.felt.common.mod.server.FeltServer;
import gr1mly4memes.felt.common.mod.util.remapper.FeltRemapConfig;
import gr1mly4memes.felt.common.mod.util.remapper.ClassLoaderRemapper;
import gr1mly4memes.felt.common.mod.util.remapper.GlobalClassRepo;
import gr1mly4memes.felt.common.mod.util.remapper.PluginTransformer;
import gr1mly4memes.felt.common.mod.util.remapper.patcher.integrated.IntegratedPatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FeltPluginPatcher implements PluginTransformer {

    private final List<PluginPatcher> list;

    public FeltPluginPatcher(List<PluginPatcher> list) {
        this.list = list;
    }

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper, FeltRemapConfig config) {
        for (PluginPatcher patcher : list) {
            patcher.handleClass(node, GlobalClassRepo.INSTANCE);
        }
    }

    public static List<PluginPatcher> load(List<PluginTransformer> transformerList) {
        var list = new ArrayList<PluginPatcher>();
        File pluginFolder = new File("plugins");
        if (pluginFolder.exists()) {
            FeltServer.LOGGER.info("Loading plugin patchers ...");
            File[] files = pluginFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".jar")) {
                        loadFromJar(file).ifPresent(list::add);
                    }
                }
                if (!list.isEmpty()) {
                    FeltServer.LOGGER.info("Loaded {} patchers", list.size());
                }
            }
        }
        list.add(new IntegratedPatcher());
        list.sort(Comparator.comparing(PluginPatcher::priority));
        transformerList.add(new FeltPluginPatcher(list));
        return list;
    }

    private static Optional<PluginPatcher> loadFromJar(File file) {
        try (JarFile jarFile = new JarFile(file)) {
            JarEntry jarEntry = jarFile.getJarEntry("plugin.yml");
            if (jarEntry != null) {
                try (InputStream stream = jarFile.getInputStream(jarEntry)) {
                    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
                    String name = configuration.getString("arclight.patcher");
                    if (name != null) {
                        URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, FeltPluginPatcher.class.getClassLoader());
                        Class<?> clazz = Class.forName(name, false, loader);
                        PluginPatcher patcher = clazz.asSubclass(PluginPatcher.class).getConstructor().newInstance();
                        return Optional.of(patcher);
                    }
                }
            }
        } catch (Throwable e) {
            FeltServer.LOGGER.debug("Error occurred while loading patcher", e);
        }
        return Optional.empty();
    }
}
