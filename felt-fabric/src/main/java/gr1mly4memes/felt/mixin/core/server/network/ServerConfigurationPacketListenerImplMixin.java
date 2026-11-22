package gr1mly4memes.felt.mixin.core.server.network;

import com.mojang.authlib.GameProfile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.server.network.ServerConfigurationPacketListenerImplBridge;
import gr1mly4memes.felt.bridge.core.server.network.config.PrepareSpawnTaskBridge;
import io.papermc.paper.connection.PaperConfigurationTask;
import io.papermc.paper.connection.PaperPlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.network.config.ServerCodeOfConductConfigurationTask;
import io.papermc.paper.event.connection.configuration.PlayerCodeOfConductSendEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServerLinks;
import org.bukkit.event.player.PlayerLinksSendEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin extends ServerCommonPacketListenerImplMixin implements ServerConfigurationPacketListenerImplBridge {

    @Mutable
    @Shadow @Final private GameProfile gameProfile;

    @Shadow private ClientInformation clientInformation;

    @Shadow @Final private Queue<ConfigurationTask> configurationTasks;

    public PaperPlayerConfigurationConnection paperConnection;
    @Unique private PaperConfigurationTask felt$configurationTask;

    @ShadowConstructor.Super
    public abstract void felt$super(MinecraftServer server, Connection connection, CommonListenerCookie cookie, ServerPlayer player);

    @CreateConstructor
    public void felt$constructor(MinecraftServer server, Connection connection, CommonListenerCookie cookie, ServerPlayer player) {
        felt$super(server, connection, cookie, player);
        this.gameProfile = cookie.gameProfile();
        this.clientInformation = cookie.clientInformation();
        this.paperConnection = new PaperPlayerConfigurationConnection((ServerConfigurationPacketListenerImpl) (Object) this);
    }

    @Inject(method = "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/network/CommonListenerCookie;)V",
        at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private void felt$initConfigurationConnection(MinecraftServer server, Connection connection, CommonListenerCookie cookie, CallbackInfo ci) {
        this.paperConnection = new PaperPlayerConfigurationConnection((ServerConfigurationPacketListenerImpl) (Object) this);
    }

    @Override
    public PlayerConfigurationConnection bridge$getConfigurationConnection() {
        return this.paperConnection;
    }

    @Invoker("finishCurrentTask")
    public abstract void felt$finishCurrentTask(ConfigurationTask.Type type);

    @Decorate(method = "addOptionalTasks()V", require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Ljava/util/Map;isEmpty()Z"))
    private boolean felt$queueCodeOfConduct(Map<?, ?> codeOfConducts) throws Throwable {
        boolean empty = (boolean) DecorationOps.callsite().invoke(codeOfConducts);
        return empty && PlayerCodeOfConductSendEvent.getHandlerList().getRegisteredListeners().length == 0;
    }

    @Decorate(method = "lambda$addOptionalTasks$0(Ljava/util/Map;)Ljava/lang/String;", require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"))
    private Object felt$emptyCodeOfConduct(Iterator<?> iterator, Map<String, String> codeOfConducts) throws Throwable {
        return codeOfConducts.isEmpty() ? null : DecorationOps.callsite().invoke(iterator);
    }

    @Decorate(method = "lambda$addOptionalTasks$0(Ljava/util/Map;)Ljava/lang/String;", require = 1, expect = 1, allow = 1,
        at = @At("RETURN"))
    private void felt$sendCodeOfConductEvent(String codeOfConduct) throws Throwable {
        var event = new PlayerCodeOfConductSendEvent(this.bridge$getConfigurationConnection(), codeOfConduct);
        event.callEvent();
        codeOfConduct = event.getCodeOfConduct();
        if (codeOfConduct == null) {
            this.felt$finishCurrentTask(ServerCodeOfConductConfigurationTask.TYPE);
        }
        DecorationOps.callsite().invoke(codeOfConduct);
    }

    @Inject(method = "addOptionalTasks()V", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private void felt$addConfigurationTask(CallbackInfo ci) {
        if (this.felt$configurationTask == null) {
            this.felt$configurationTask = new PaperConfigurationTask((ServerConfigurationPacketListenerImpl) (Object) this);
            this.configurationTasks.add(this.felt$configurationTask);
        }
    }

    @Inject(method = "onDisconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("HEAD"), require = 1, expect = 1, allow = 1)
    private void felt$closeConfigurationTask(DisconnectionDetails details, CallbackInfo ci) {
        if (this.felt$configurationTask != null) {
            this.felt$configurationTask.close();
        }
    }

    @Inject(method = "onDisconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private void felt$retireConfigurationPlayerScheduler(DisconnectionDetails details, CallbackInfo ci) {
        if (this.player == null || this.connection.getPacketListener() != (Object) this) {
            return;
        }
        var bridge = (ServerPlayerBridge) this.player;
        if (bridge.bridge$getTransferCookieConnection() != (Object) this) {
            return;
        }
        // Login already created this wrapper, and preparePlayerData preserves it across replacement.
        // After the game listener takes ownership, PlayerList.remove is responsible for retirement.
        var craft = bridge.bridge$getBukkitEntity();
        if (craft.getHandleRaw() == this.player && !craft.taskScheduler.isRetired()) {
            craft.taskScheduler.retire();
        }
    }

    @Decorate(method = "handleResourcePackResponse(Lnet/minecraft/network/protocol/common/ServerboundResourcePackPacket;)V", require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;finishCurrentTask(Lnet/minecraft/server/network/ConfigurationTask$Type;)V"))
    private void felt$finishServerResourcePackTask(ServerConfigurationPacketListenerImpl listener, ConfigurationTask.Type type, ServerboundResourcePackPacket packet) throws Throwable {
        if (packet.id().equals(this.server.getServerResourcePack().map(MinecraftServer.ServerResourcePackInfo::id).orElse(null))) {
            DecorationOps.callsite().invoke(listener, type);
        }
    }

    @WrapOperation(method = "startNextTask()V", require = 1, expect = 1, allow = 1,
        at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.PUTFIELD, target = "Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;currentTask:Lnet/minecraft/server/network/ConfigurationTask;"))
    private void felt$bindSpawnPlayer(ServerConfigurationPacketListenerImpl listener, ConfigurationTask task, Operation<Void> original) {
        if (task instanceof PrepareSpawnTask) {
            ((PrepareSpawnTaskBridge) task).bridge$setSpawnPlayer(this.bridge$getPlayer());
        }
        original.call(listener, task);
    }

    @Decorate(method = {"startConfiguration", "runConfiguration", "vanillaStart"}, require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;serverLinks()Lnet/minecraft/server/ServerLinks;"))
    private ServerLinks felt$sendLinksEvent(MinecraftServer instance) throws Throwable {
        var links = (ServerLinks) DecorationOps.callsite().invoke(instance);
        var wrapper = new CraftServerLinks(links);
        var event = new PlayerLinksSendEvent(this.bridge$getConfigurationConnection(), wrapper);
        Bukkit.getPluginManager().callEvent(event);
        return wrapper.getServerLinks();
    }
}
