package gr1mly4memes.felt.common.mixin.bukkit;

import gr1mly4memes.felt.common.mod.server.world.FeltWorldConfig;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spigotmc.SpigotConfig;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpigotWorldConfig.class, remap = false)
public class SpigotWorldConfigMixin {

    @Shadow private String worldName;
    @Shadow @Final @Mutable private YamlConfiguration config;

    @Inject(method = "init()V", at = @At("HEAD"))
    private void arclight$refreshConfig(CallbackInfo ci) {
        // SpigotConfig.init replaces the global YAML object on reload; rebind this
        // existing per-world config before its normal init reads values from it.
        this.config = SpigotConfig.config;
    }

    @SuppressWarnings("StringEquality")
    @Decorate(method = "log", inject = true, at = @At("HEAD"))
    private void arclight$skipLog(String content) throws Throwable {
        if (worldName == FeltWorldConfig.DEFAULT_MARKER) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke();
    }
}
