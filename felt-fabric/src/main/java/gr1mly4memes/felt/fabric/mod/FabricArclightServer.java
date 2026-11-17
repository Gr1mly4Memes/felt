package gr1mly4memes.felt.fabric.mod;

import gr1mly4memes.felttools.api.FeltServer;
import gr1mly4memes.felttools.api.TickingTracker;
import gr1mly4memes.felt.common.mod.server.api.DefaultTickingTracker;
import org.bukkit.plugin.Plugin;

public class FabricArclightServer implements FeltServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

    public void registerForgeEvent(Plugin plugin, net.minecraftforge.eventbus.api.IEventBus eventBus, Object target) {
        registerModEvent(plugin, eventBus, target);
    }

    @Override
    public void registerModEvent(Plugin plugin, Object bus, Object target) {
        throw new UnsupportedOperationException("Not supported on Fabric");
    }

    @Override
    public TickingTracker getTickingTracker() {
        return this.tickingTracker;
    }
}
