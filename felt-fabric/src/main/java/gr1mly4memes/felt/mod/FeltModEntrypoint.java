package gr1mly4memes.felt.mod;

import gr1mly4memes.felttools.api.Felt;
import gr1mly4memes.felt.mod.FabricFeltServer;
import gr1mly4memes.felt.mod.event.EventHandlerRegistry;
import gr1mly4memes.felt.mod.permission.FeltPermissionImpl;
import net.fabricmc.api.ModInitializer;

public class FeltModEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        Felt.setServer(new FabricFeltServer());
        EventHandlerRegistry.register();
        FeltPermissionImpl.init();
    }
}