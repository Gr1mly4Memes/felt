package gr1mly4memes.felt.mod.util.remapper;

import org.objectweb.asm.tree.ClassNode;

public interface PluginTransformer {

    void handleClass(ClassNode node, ClassLoaderRemapper remapper, FeltRemapConfig config);

    default int priority() {
        return 0;
    }
}
