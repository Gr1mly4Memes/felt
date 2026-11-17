package gr1mly4memes.felt.common.mod.util.remapper;

import gr1mly4memes.felttools.api.PluginPatcher;
import gr1mly4memes.felt.common.mod.util.remapper.patcher.FeltPluginPatcher;
import gr1mly4memes.felt.common.mod.util.remapper.patcher.PluginLoggerTransformer;
import gr1mly4memes.felt.common.mod.util.remapper.resource.RemapSourceHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class FeltRemapper {

    public static final FeltRemapper INSTANCE;
    public static final File DUMP;
    public static final Function<byte[], byte[]> SWITCH_TABLE_FIXER;

    static {
        try {
            INSTANCE = new FeltRemapper();
            String property = System.getProperty("arclight.remapper.dump");
            if (property != null) {
                DUMP = new File(property);
                if (!DUMP.exists()) {
                    DUMP.mkdirs();
                }
                try {
                    FileUtils.forceDelete(DUMP);
                } catch (IOException ignored) {
                }
            } else {
                DUMP = null;
            }
            SWITCH_TABLE_FIXER = (Function<byte[], byte[]>) Class.forName("gr1mly4memes.felt.boot.asm.SwitchTableFixer").getField("INSTANCE").get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final List<PluginTransformer> transformerList = new ArrayList<>();
    private final List<PluginPatcher> patchers;

    public FeltRemapper() {
        this.transformerList.add(FeltRedirectAdapter.INSTANCE);
        this.transformerList.add(ClassLoaderAdapter.INSTANCE);
        if (!(java.util.logging.LogManager.getLogManager() instanceof org.apache.logging.log4j.jul.LogManager)) {
            this.transformerList.add(new PluginLoggerTransformer());
        }
        this.patchers = FeltPluginPatcher.load(this.transformerList);
        RemapSourceHandler.register();
    }

    public static ClassLoaderRemapper createClassLoaderRemapper(ClassLoader classLoader) {
        return new ClassLoaderRemapper(classLoader);
    }

    public List<PluginTransformer> getTransformerList() {
        return transformerList;
    }

    public List<PluginPatcher> getPatchers() {
        return patchers;
    }
}
