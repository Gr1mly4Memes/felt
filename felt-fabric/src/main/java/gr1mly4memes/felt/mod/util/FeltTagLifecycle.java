package gr1mly4memes.felt.mod.util;

import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent.Cause;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import org.jetbrains.annotations.Nullable;

public final class FeltTagLifecycle {
    private static final ThreadLocal<Cause> CAUSE = new ThreadLocal<>();
    private static final ThreadLocal<Registry<?>> REGISTRY = new ThreadLocal<>();

    private FeltTagLifecycle() {}

    public static Cause cause() {
        Cause cause = CAUSE.get();
        return cause == null ? Cause.INITIAL : cause;
    }

    public static <T> T withCause(Cause cause, Supplier<T> load) {
        return withValue(CAUSE, cause, load);
    }

    public static Executor reloadExecutor(Executor executor) {
        return task -> executor.execute(() -> withCause(Cause.RELOAD, () -> {
            task.run();
            return null;
        }));
    }

    public static <T> T withRegistry(Registry<?> registry, Supplier<T> load) {
        return withValue(REGISTRY, registry, load);
    }

    public static @Nullable Registry<?> takeRegistry() {
        Registry<?> registry = REGISTRY.get();
        REGISTRY.remove();
        return registry;
    }

    private static <T, V> T withValue(ThreadLocal<V> scope, V value, Supplier<T> action) {
        V previous = scope.get();
        scope.set(value);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                scope.remove();
            } else {
                scope.set(previous);
            }
        }
    }
}
