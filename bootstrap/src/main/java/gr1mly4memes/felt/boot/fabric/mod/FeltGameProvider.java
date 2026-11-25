package gr1mly4memes.felt.boot.fabric.mod;

import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.Arguments;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class FeltGameProvider extends MinecraftGameProvider {

    private Path modFile;

    @Override
    public void initialize(FabricLauncher launcher) {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("log4j.jul.LoggerAdapter", "gr1mly4memes.felt.boot.log.FeltLoggerAdapter");
        System.setProperty("log4j.configurationFile", "felt-log4j2.xml");
        try {
            this.modFile = this.extract();
            launcher.addToClassPath(modFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        var extraClasspath = System.getProperty("felt.fabric.classpath", "");
        if (!extraClasspath.isEmpty()) for (var lib : extraClasspath.split(File.pathSeparator)) {
            launcher.addToClassPath(Paths.get(lib));
        }
        super.initialize(launcher);
    }

    @Override
    public Arguments getArguments() {
        Arguments arguments = super.getArguments();
        String old = arguments.get(Arguments.ADD_MODS);
        var builtinMods = System.getProperty("felt.fabric.builtinMods");
        var path = this.modFile.toString() + (builtinMods == null || builtinMods.isEmpty() ? "" : File.pathSeparator + builtinMods);
        if (old != null) {
            path = old + File.pathSeparator + path;
        }
        arguments.put(Arguments.ADD_MODS, path);
        return arguments;
    }

    private String getFeltVersion() throws Exception {
        try (var stream = getClass().getResourceAsStream("/META-INF/MANIFEST.MF")) {
            var manifest = new Manifest(stream);
            var attributes = manifest.getMainAttributes();
            return attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        }
    }

    private Path extract() throws Exception {
        var version = getFeltVersion();
        System.setProperty("felt.version", version);
        var dir = Paths.get(".felt", "mod_file");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        var mod = dir.resolve(version + ".jar");
        var staged = Files.createTempFile(dir, ".common-", ".jar");
        try {
            try (var path = getClass().getResourceAsStream("/common.jar")) {
                if (path == null) throw new IllegalStateException("Felt common.jar is missing");
                Files.copy(path, staged, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            // Snapshot rebuilds can share a version and commit, so compare content.
            if (!Files.exists(mod) || Boolean.getBoolean("felt.alwaysExtract") || Files.mismatch(staged, mod) != -1L) {
                try {
                    Files.move(staged, mod, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                    Files.move(staged, mod, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                try (var files = Files.list(dir)) {
                    for (Path old : files.filter(file -> !file.equals(mod) && !file.equals(staged)).toList()) {
                        Files.delete(old);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(staged);
        }
        return mod;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        super.unlockClassPath(launcher);
        try {
            var field = launcher.getClass().getDeclaredField("unlocked");
            field.setAccessible(true);
            field.set(launcher, true);
            var ctor = launcher.loadIntoTarget("gr1mly4memes.felt.boot.FabricBootstrap").getConstructor();
            ((Consumer<FabricLauncher>) ctor.newInstance()).accept(launcher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getRawGameVersion() {
        try {
            return super.getRawGameVersion() + " Felt " + getFeltVersion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
