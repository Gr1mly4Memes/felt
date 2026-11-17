package gr1mly4memes.felt.common.mixin.core.server.dedicated;

import gr1mly4memes.felt.common.bridge.core.server.dedicated.DedicatedServerSettingsBridge;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DedicatedServerSettings.class)
public abstract class DedicatedServerSettingsMixin implements DedicatedServerSettingsBridge {
    @Override @Accessor("source")
    public abstract java.nio.file.Path bridge$getSource();
}
