package gr1mly4memes.felt.mixin.bukkit;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import gr1mly4memes.felt.bridge.bukkit.MessengerBridge;
import gr1mly4memes.felt.mod.plugin.messaging.PacketRecorder;
import gr1mly4memes.felt.mod.server.FeltServer;
import gr1mly4memes.felt.mod.plugin.messaging.FeltPluginChannel;
import net.minecraft.resources.Identifier;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin implements Messenger, MessengerBridge {

    @Shadow @Final private Map<String, Set<Plugin>> outgoingByChannel;

    @Shadow @Final private Map<String, Set<PluginMessageListenerRegistration>> incomingByChannel;

    @Unique
    private Map<Identifier, FeltPluginChannel<?>> felt$registry;

    @Unique
    private SetMultimap<Plugin, Identifier> felt$crossSend;

    @Unique
    private PacketRecorder felt$recorder;

    @ModifyExpressionValue(method = "validateAndCorrectChannel", at = @At(value = "FIELD",
            target = "Lorg/bukkit/plugin/messaging/Messenger;MAX_CHANNEL_SIZE:I"))
    private static int felt$modifyMaxChannelSize(int original) {
        return 256;
    }

    @Override
    public FeltPluginChannel<?> felt$getAndCheckCrossSend(Plugin src, Identifier channel) {
        var felt = this.felt$registry.get(channel);
        if (src == null) {
            FeltServer.LOGGER.warn("Sending anonymous packet on channel {}", channel);
        } else if (!felt.getOutgoing().contains(src)) {
            boolean first;
            synchronized (this.felt$crossSend) {
                first = this.felt$crossSend.put(src, channel);
            }
            if (first) {
                FeltServer.LOGGER.warn("A plugin is sending message on a channel that's registered as outgoing by other plugins but itself!");
                FeltServer.LOGGER.warn("Plugin: [{}], on channel: {}", src.getPluginMeta().getDisplayName(), channel);
                FeltServer.LOGGER.warn("This warning will only be displayed once for every plugin and channel.");
            }
        }
        return felt;
    }

    @Override
    public void felt$checkUnsafeSend(Plugin src, Identifier channel) {
        var felt = felt$registry.get(channel);
        if (felt != null && !felt.getOutgoing().isEmpty()) {
            return;
        }
        var fullName = src == null ? "Unknown" : src.getPluginMeta().getDisplayName();
        if (src == null) {
            felt$registerAnonymousOutgoing(channel);
        } else {
            registerOutgoingPluginChannel(src, channel.toString());
        }
        FeltServer.LOGGER.warn("Plugin [{}] is sending message on an unregistered outgoing channel {}, registering.", fullName, channel);
    }

    @Override
    public void felt$sendCustomPayload(Plugin src, CraftPlayer dst, Identifier location, byte[] data) {
        felt$checkUnsafeSend(src, location);
        var channel = felt$getAndCheckCrossSend(src, location);
        channel.sendCustomPayload(src, dst, data);
    }

    @Override
    public void felt$registerAnonymousOutgoing(Identifier location) {
        felt$updateChannel(location, true);
    }

    @Override
    public PacketRecorder felt$getPacketRecorder() {
        return felt$recorder;
    }

    @Unique
    private void felt$updateChannel(Identifier location, boolean create) {
        if (location != null) {
            var id = location.toString();
            var channel = felt$registry.computeIfAbsent(location, it -> {
                if (!create) {
                    return null;
                }
                var inByChannel = incomingByChannel.computeIfAbsent(id, k -> new HashSet<>());
                var outByChannel = outgoingByChannel.computeIfAbsent(id, k -> new HashSet<>());
                return felt$setupChannel(location, inByChannel, outByChannel);
            });
            if (channel != null) {
                channel.getChannelHandler().updateChannel();
            }
        }
    }

    @Unique
    private void felt$updateChannel(String location, boolean create) {
        felt$updateChannel(Identifier.tryParse(location), create);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void felt$init(CallbackInfo ci) {
        felt$registry = new HashMap<>();
        felt$crossSend = MultimapBuilder.hashKeys().hashSetValues().build();
        felt$recorder = new PacketRecorder();
    }

    @Redirect(method = {"removeFromOutgoing*", "removeFromIncoming*"}, at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object felt$skipRemove(Map<?, ?> thus, Object key) {
        return null;
    }

    @Inject(method = "addToOutgoing", at = @At("RETURN"))
    private void felt$registerOut(Plugin plugin, String id, CallbackInfo ci) {
        felt$updateChannel(id, true);
    }

    @Inject(method = "removeFromOutgoing(Lorg/bukkit/plugin/Plugin;Ljava/lang/String;)V", at = @At("RETURN"))
    private void felt$unregisterOut(Plugin plugin, String id, CallbackInfo ci) {
        felt$updateChannel(id, false);
    }

    @Inject(method = "addToIncoming", at = @At("RETURN"))
    private void felt$registerIn(PluginMessageListenerRegistration registration, CallbackInfo ci) {
        felt$updateChannel(registration.getChannel(), true);
    }

    @Inject(method = "removeFromIncoming(Lorg/bukkit/plugin/messaging/PluginMessageListenerRegistration;)V", at = @At("RETURN"))
    private void felt$unregisterIn(PluginMessageListenerRegistration registration, CallbackInfo ci) {
        felt$updateChannel(registration.getChannel(), false);
    }

    @Inject(method = "validateAndCorrectChannel", at = @At("TAIL"))
    private static void felt$enhancedValidation(String channel, CallbackInfoReturnable<String> cir) {
        if (!valid.containsKey(channel)) {
            var corrected = cir.getReturnValue();
            var namespace = corrected.substring(0, corrected.indexOf(':'));
            var path = corrected.substring(corrected.indexOf(':') + 1);
            if (!Identifier.isValidNamespace(namespace) || !Identifier.isValidPath(path)) {
                FeltServer.LOGGER.warn("Channel name is malformed and impossible to use: {}", corrected);
                FeltServer.LOGGER.warn("Related functionality cannot be guaranteed!");
                FeltServer.LOGGER.warn("This message will only be displayed once for this channel!");
                valid.put(channel, false);
            } else {
                valid.put(channel, true);
            }
        }
    }
}