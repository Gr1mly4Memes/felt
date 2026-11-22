package gr1mly4memes.felt.mixin.core.server;

import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felt.mod.util.BukkitOptionParser;
import io.papermc.paper.plugin.PluginInitializerManager;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {

    @Inject(method = "main([Ljava/lang/String;)V", at = @At("HEAD"), remap = false)
    private static void felt$captureBukkitOptions(String[] args, CallbackInfo ci) {
        FeltCaptures.captureBukkitOptions(new BukkitOptionParser().parse(args));
    }

    @Decorate(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljoptsimple/OptionParser;parse([Ljava/lang/String;)Ljoptsimple/OptionSet;"), remap = false, require = 1, expect = 1, allow = 1)
    private static OptionSet felt$parseNativeOptions(OptionParser parser, String[] args) throws Throwable {
        return (OptionSet) DecorationOps.callsite().invoke(parser, BukkitOptionParser.filterNativeArguments(parser, FeltCaptures.getBukkitOptions(), args));
    }

    @Decorate(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"), remap = false, require = 1, expect = 1, allow = 1)
    private static net.minecraft.server.packs.repository.PackRepository felt$refreshBuiltinPacks(
        net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess access,
        @gr1mly4memes.felttools.mixin.Local(ordinal = -1) boolean safeModeEnabled) throws Throwable {
        var repository = (net.minecraft.server.packs.repository.PackRepository) DecorationOps.callsite().invoke(access);
        if (!safeModeEnabled) {
            io.papermc.paper.datapack.DynamicBuiltinPacks.refreshAllMetadata(access);
        }
        return repository;
    }

    @Inject(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;bootStrap()V"), remap = false)
    private static void felt$loadPluginProviders(String[] args, CallbackInfo ci) throws Exception {
        PluginInitializerManager.load(FeltCaptures.getBukkitOptions());
    }
}