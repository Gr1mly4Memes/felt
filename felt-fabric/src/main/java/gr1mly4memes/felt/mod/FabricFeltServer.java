package gr1mly4memes.felt.mod;

import gr1mly4memes.felttools.api.FeltServer;
import gr1mly4memes.felttools.api.TickingTracker;
import gr1mly4memes.felt.mod.server.api.DefaultTickingTracker;
import org.bukkit.plugin.Plugin;

public class FabricFeltServer implements FeltServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

    @Override
    public void registerModEvent(Plugin plugin, Object bus, Object target) {
        throw new UnsupportedOperationException("Not supported on Fabric");
    }

    @Override
    public TickingTracker getTickingTracker() {
        return this.tickingTracker;
    }
}
