package gr1mly4memes.felt.boot;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/** Strip Loom's compile ABI methods before Mixin caches target-class metadata in development. */
public final class DevelopmentStubFilter extends GameTransformer {
    private static final String MARKER = "Lgr1mly4memes/felt/mod/mixins/annotation/CompileOnlyStub;";
    private static final byte[] MARKER_BYTES = MARKER.getBytes(StandardCharsets.UTF_8);
    private final GameTransformer delegate;
    private final MethodHandle rawBytes;

    public static void install() {
        if (!Boolean.getBoolean("fabric.development")) return;
        try {
            var provider = FabricLoaderImpl.INSTANCE.getGameProvider();
            var transformer = MinecraftGameProvider.class.getDeclaredField("transformer");
            transformer.setAccessible(true);
            var current = (GameTransformer) transformer.get(provider);
            if (!(current instanceof DevelopmentStubFilter)) {
                // Loom's ordinary launcher does not invoke FeltGameProvider's felt_boot entrypoint.
                // Keep Brigadier state, enum handling and thread checks identical to a packaged server.
                if (!(current instanceof FeltImplementer)) {
                    new FabricBootstrap().accept(FabricLauncherBase.getLauncher());
                    current = (GameTransformer) transformer.get(provider);
                }
                transformer.set(provider, new DevelopmentStubFilter(current, FabricLauncherBase.getLauncher()));
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot prepare Felt's development classpath", failure);
        }
    }

    private DevelopmentStubFilter(GameTransformer delegate, FabricLauncher launcher) throws ReflectiveOperationException {
        this.delegate = delegate;
        var field = launcher.getClass().getDeclaredField("classLoader");
        field.setAccessible(true);
        var loader = field.get(launcher);
        var method = loader.getClass().getDeclaredMethod("getRawClassByteArray", String.class, boolean.class);
        method.setAccessible(true);
        this.rawBytes = MethodHandles.lookup().unreflect(method).bindTo(loader);
    }

    @Override public void locateEntrypoints(FabricLauncher launcher, List<Path> gameJars) {
        this.delegate.locateEntrypoints(launcher, gameJars);
    }

    @Override public byte[] transform(String className) {
        byte[] bytes = this.delegate.transform(className);
        if (bytes == null) {
            try { bytes = (byte[]) this.rawBytes.invokeExact(className, false); }
            catch (Throwable failure) { throw new IllegalStateException("Cannot read development class " + className, failure); }
        }
        if (bytes == null || !containsMarker(bytes)) return bytes;
        var node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        boolean removed = node.methods.removeIf(method -> method.invisibleAnnotations != null
            && method.invisibleAnnotations.stream().anyMatch(annotation -> MARKER.equals(annotation.desc)));
        if (!removed) return bytes;
        var writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean containsMarker(byte[] bytes) {
        outer: for (int i = 0; i <= bytes.length - MARKER_BYTES.length; i++) {
            for (int j = 0; j < MARKER_BYTES.length; j++) if (bytes[i + j] != MARKER_BYTES[j]) continue outer;
            return true;
        }
        return false;
    }
}
