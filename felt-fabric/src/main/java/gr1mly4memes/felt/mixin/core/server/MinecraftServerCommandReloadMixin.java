package gr1mly4memes.felt.mixin.core.server;

import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felt.bridge.core.server.MinecraftServerBridge;
import gr1mly4memes.felt.bridge.core.server.ReloadableServerResourcesBridge;
import gr1mly4memes.felt.mod.util.PaperCommandLifecycle;
import io.papermc.paper.command.brigadier.PaperBrigadier;
import io.papermc.paper.command.brigadier.PaperCommands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.players.PlayerList;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.help.SimpleHelpMap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerCommandReloadMixin {

    @Shadow private MinecraftServer.ReloadableResources resources;

    @Unique private boolean felt$initialBootstrapCommandsFired;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$bindInitialPaperCommands(CallbackInfo ci) {
        // CraftCommandMap inserts defaults during CraftServer's field initializers,
        // before its constructor RETURN. The native server already owns this stem.
        felt$bindPaperCommands(this.resources.managers());
    }

    @Inject(method = "setPlayerList(Lnet/minecraft/server/players/PlayerList;)V", at = @At("RETURN"),
        require = 1, expect = 1, allow = 1)
    private void felt$initialBootstrapCommands(PlayerList playerList, CallbackInfo ci) {
        // PlayerList's constructor has now created CraftServer (including CommandMap
        // defaults), but DedicatedServer has not entered the main-plugin providers yet.
        if (this.felt$initialBootstrapCommandsFired) {
            return;
        }
        this.felt$initialBootstrapCommandsFired = true;
        PaperCommandLifecycle.bootstrap(ReloadableRegistrarEvent.Cause.INITIAL);
    }

    @Decorate(method = "lambda$reloadResources$4(Ljava/util/Collection;Lnet/minecraft/server/MinecraftServer$ReloadableResources;)V", at = @At(value = "FIELD",
        target = "Lnet/minecraft/server/MinecraftServer;resources:Lnet/minecraft/server/MinecraftServer$ReloadableResources;",
        opcode = Opcodes.PUTFIELD), require = 1, expect = 1, allow = 1)
    private void felt$adoptPaperCommands(MinecraftServer server, MinecraftServer.ReloadableResources next) throws Throwable {
        Commands previousCommands = this.resources.managers().getCommands();
        // This is the native successful-load continuation, not loadResources RETURN
        // (which only returns a future). A failed load never reaches this field write.
        // Native code does not roll this assignment back if later finalization fails;
        // Paper must keep following the resources actually adopted in that case too.
        DecorationOps.callsite().invoke(server, next);
        felt$bindPaperCommands(next.managers());
        // The forwarding CommandMap has no independent storage. Transfer its real
        // Bukkit nodes, including aliases/defaults, before the existing sync hook.
        // Do not construct another Commands: mod loader registration runs only once.
        PaperBrigadier.moveBukkitCommands(previousCommands, next.managers().getCommands());
    }

    @Inject(method = "lambda$reloadResources$4(Ljava/util/Collection;Lnet/minecraft/server/MinecraftServer$ReloadableResources;)V", at = @At("RETURN"),
        require = 1, expect = 1, allow = 1)
    private void felt$replayPaperCommands(java.util.Collection<String> packs,
            MinecraftServer.ReloadableResources next, CallbackInfo ci) {
        // Finish the native continuation first: loading, close, adoption and all
        // native reload finalizers must succeed before any plugin handler is replayed.
        // The adoption hook above has already restored defaults and legacy aliases.
        PaperCommandLifecycle.bootstrap(ReloadableRegistrarEvent.Cause.RELOAD);
        PaperCommandLifecycle.plugins(ReloadableRegistrarEvent.Cause.RELOAD);

        CraftServer craftServer = ((MinecraftServerBridge) this).bridge$getServer();
        SimpleHelpMap helpMap = (SimpleHelpMap) craftServer.getHelpMap();
        helpMap.clear();
        helpMap.initializeGeneralTopics();
        helpMap.initializeCommands();
        craftServer.syncCommands();
    }

    @Unique
    private static void felt$bindPaperCommands(ReloadableServerResources resources) {
        PaperCommands.INSTANCE.setDispatcher(resources.getCommands(),
            ((ReloadableServerResourcesBridge) resources).bridge$getCommandBuildContext());
        try {
            io.papermc.paper.command.PaperCommands.registerCommands();
        } finally {
            // setDispatcher opens registration for the internal command setup above;
            // retained plugin registrars must stay closed until their event is fired.
            PaperCommands.INSTANCE.setCurrentContext(null);
            PaperCommands.INSTANCE.invalidate();
        }
    }
}
