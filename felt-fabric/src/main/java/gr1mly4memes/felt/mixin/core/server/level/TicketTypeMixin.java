package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.bridge.core.server.level.TicketTypeBridge;
import gr1mly4memes.felt.mod.mixins.annotation.TransformAccess;
import net.minecraft.server.level.TicketType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketType.class)
public abstract class TicketTypeMixin implements TicketTypeBridge {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static long pluginTimeout = 0L;
    private static final TicketType FELT_CHUNK_LOAD = felt$register("chunk_load", TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING);
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
    private static final TicketType PLUGIN = felt$register("plugin", TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
    private static final TicketType PLUGIN_TICKET = felt$register("plugin_ticket", TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

    static {
        if ((Object) PLUGIN_TICKET instanceof ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType<?> type) {
            @SuppressWarnings("unchecked")
            var pluginType = (ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType<org.bukkit.plugin.Plugin>) type;
            pluginType.moonrise$setIdentifierComparator(java.util.Comparator.comparing(org.bukkit.plugin.Plugin::getName));
        }
    }

    @Invoker("register")
    private static TicketType felt$register(String name, long timeout, int flags) {
        throw new AssertionError();
    }

    /**
     * @author IzzelAliz
     * @reason CraftBukkit exposes chunk-gc.period-in-ticks through TicketType.PLUGIN.
     */
    @Overwrite
    public long timeout() {
        return (Object) this == PLUGIN ? pluginTimeout : this.felt$timeout();
    }

    @Override
    public void bridge$setPluginTimeout(long timeout) {
        pluginTimeout = timeout;
    }

    @Accessor("timeout")
    public abstract long felt$timeout();

    @Override @Accessor(value = "timeout")
    public abstract void bridge$setLifespan(long lifespan);
}
