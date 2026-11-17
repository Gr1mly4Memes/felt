package gr1mly4memes.felt.fabric;

import gr1mly4memes.felttools.api.Felt;
import gr1mly4memes.felt.fabric.mod.FabricArclightServer;
import gr1mly4memes.felt.fabric.mod.event.EventHandlerRegistry;
import gr1mly4memes.felt.fabric.mod.permission.FeltPermissionImpl;
import net.fabricmc.api.ModInitializer;

public class FeltModEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        Felt.setServer(new FabricArclightServer());
        EventHandlerRegistry.register();
        FeltPermissionImpl.init();
    }
}