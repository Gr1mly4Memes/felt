package gr1mly4memes.felt.mod;

import gr1mly4memes.felt.mod.FeltCommon;
import gr1mly4memes.felt.mod.FeltMixinPlugin;
import gr1mly4memes.felttools.api.FeltPlatform;
import gr1mly4memes.felt.boot.AbstractBootstrap;
import gr1mly4memes.felttools.mixin.MixinTools;
import org.apache.logging.log4j.LogManager;

public class FabricMixinPlugin extends FeltMixinPlugin implements AbstractBootstrap {

    @Override
    public void onLoad(String mixinPackage) {
        gr1mly4memes.felt.boot.DevelopmentStubFilter.install();
        FeltCommon.setInstance(new FabricCommonImpl());
        super.onLoad(mixinPackage);
        MixinTools.setup();
        try {
            this.setupMod(FeltPlatform.FABRIC, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(LogManager::shutdown, "log flusher"));
    }
}
