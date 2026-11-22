package gr1mly4memes.felt.bridge.bukkit;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.SimplePluginManager;

import java.util.logging.Logger;

public interface PluginClassLoaderBridge {
    PluginDescriptionFile felt$desc();
    Class<?> felt$loadFromExternal(String name, boolean initialize, boolean checkLibraries) throws ClassNotFoundException;
    SimplePluginManager felt$getPluginManager();
    Logger felt$systemLogger();
}
