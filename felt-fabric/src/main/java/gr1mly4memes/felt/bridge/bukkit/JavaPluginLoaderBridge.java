package gr1mly4memes.felt.bridge.bukkit;


import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;

import java.net.URLClassLoader;
import java.util.List;

public interface JavaPluginLoaderBridge {

    Server felt$server();

    <T extends URLClassLoader & PluginClassLoaderBridge> List<T> felt$getLoaders();

    void bridge$setClass(final String name, final Class<?> clazz);

    Class<?> felt$getClassByName(String name, boolean resolve, PluginDescriptionFile description);
}
