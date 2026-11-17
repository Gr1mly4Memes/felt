package gr1mly4memes.felt.common.mixin.bukkit;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import gr1mly4memes.felt.common.bridge.bukkit.MessengerBridge;
import gr1mly4memes.felt.common.mod.plugin.messaging.PacketRecorder;
import gr1mly4memes.felt.common.mod.server.FeltServer;
import gr1mly4memes.felt.common.mod.plugin.messaging.FeltPluginChannel;
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
    private Map<Identifier, FeltPluginChannel<?>> arclight$registry;

    @Unique
    private SetMultimap<Plugin, Identifier> arclight$crossSend;

    @Unique
    private PacketRecorder arclight$recorder;

    @ModifyExpressionValue(method = "validateAndCorrectChannel", at = @At(value = "FIELD",
            target = "Lorg/bukkit/plugin/messaging/Messenger;MAX_CHANNEL_SIZE:I"))
    private static int arclight$modifyMaxChannelSize(int original) {
        return 256;
    }

    @Override
    public FeltPluginChannel<?> arclight$getAndCheckCrossSend(Plugin src, Identifier channel) {
        var arclight = this.arclight$registry.get(channel);
        if (src == null) {
            FeltServer.LOGGER.warn("Sending anonymous packet on channel {}", channel);
        } else if (!arclight.getOutgoing().contains(src)) {
            boolean first;
            synchronized (this.arclight$crossSend) {
                first = this.arclight$crossSend.put(src, channel);
            }
            if (first) {
                FeltServer.LOGGER.warn("A plugin is sending message on a channel that's registered as outgoing by other plugins but itself!");
                FeltServer.LOGGER.warn("Plugin: [{}], on channel: {}", src.getPluginMeta().getDisplayName(), channel);
                FeltServer.LOGGER.warn("This warning will only be displayed once for every plugin and channel.");
            }
        }
        return arclight;
    }

    @Override
    public void arclight$checkUnsafeSend(Plugin src, Identifier channel) {
        var arclight = arclight$registry.get(channel);
        if (arclight != null && !arclight.getOutgoing().isEmpty()) {
            return;
        }
        var fullName = src == null ? "Unknown" : src.getPluginMeta().getDisplayName();
        if (src == null) {
            arclight$registerAnonymousOutgoing(channel);
        } else {
            registerOutgoingPluginChannel(src, channel.toString());
        }
        FeltServer.LOGGER.warn("Plugin [{}] is sending message on an unregistered outgoing channel {}, registering.", fullName, channel);
    }

    @Override
    public void arclight$sendCustomPayload(Plugin src, CraftPlayer dst, Identifier location, byte[] data) {
        arclight$checkUnsafeSend(src, location);
        var channel = arclight$getAndCheckCrossSend(src, location);
        channel.sendCustomPayload(src, dst, data);
    }

    @Override
    public void arclight$registerAnonymousOutgoing(Identifier location) {
        arclight$updateChannel(location, true);
    }

    @Override
    public PacketRecorder arclight$getPacketRecorder() {
        return arclight$recorder;
    }

    @Unique
    private void arclight$updateChannel(Identifier location, boolean create) {
        if (location != null) {
            var id = location.toString();
            var channel = arclight$registry.computeIfAbsent(location, it -> {
                if (!create) {
                    return null;
                }
                var inByChannel = incomingByChannel.computeIfAbsent(id, k -> new HashSet<>());
                var outByChannel = outgoingByChannel.computeIfAbsent(id, k -> new HashSet<>());
                return arclight$setupChannel(location, inByChannel, outByChannel);
            });
            if (channel != null) {
                channel.getChannelHandler().updateChannel();
            }
        }
    }

    @Unique
    private void arclight$updateChannel(String location, boolean create) {
        arclight$updateChannel(Identifier.tryParse(location), create);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void arclight$init(CallbackInfo ci) {
        arclight$registry = new HashMap<>();
        arclight$crossSend = MultimapBuilder.hashKeys().hashSetValues().build();
        arclight$recorder = new PacketRecorder();
    }

    @Redirect(method = {"removeFromOutgoing*", "removeFromIncoming*"}, at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object arclight$skipRemove(Map<?, ?> thus, Object key) {
        return null;
    }

    @Inject(method = "addToOutgoing", at = @At("RETURN"))
    private void arclight$registerOut(Plugin plugin, String id, CallbackInfo ci) {
        arclight$updateChannel(id, true);
    }

    @Inject(method = "removeFromOutgoing(Lorg/bukkit/plugin/Plugin;Ljava/lang/String;)V", at = @At("RETURN"))
    private void arclight$unregisterOut(Plugin plugin, String id, CallbackInfo ci) {
        arclight$updateChannel(id, false);
    }

    @Inject(method = "addToIncoming", at = @At("RETURN"))
    private void arclight$registerIn(PluginMessageListenerRegistration registration, CallbackInfo ci) {
        arclight$updateChannel(registration.getChannel(), true);
    }

    @Inject(method = "removeFromIncoming(Lorg/bukkit/plugin/messaging/PluginMessageListenerRegistration;)V", at = @At("RETURN"))
    private void arclight$unregisterIn(PluginMessageListenerRegistration registration, CallbackInfo ci) {
        arclight$updateChannel(registration.getChannel(), false);
    }

    @Inject(method = "validateAndCorrectChannel", at = @At("TAIL"))
    private static void arclight$enhancedValidation(String channel, CallbackInfoReturnable<String> cir) {
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