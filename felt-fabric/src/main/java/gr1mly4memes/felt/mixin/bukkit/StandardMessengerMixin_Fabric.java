package gr1mly4memes.felt.mixin.bukkit;

import gr1mly4memes.felt.mod.plugin.messaging.FeltPluginChannel;
import gr1mly4memes.felt.mod.plugin.messaging.FeltFabricMessaging;
import gr1mly4memes.felt.bridge.bukkit.MessengerBridge;
import net.minecraft.resources.Identifier;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Set;

@Mixin(value = StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin_Fabric implements Messenger, MessengerBridge {

    @Override
    public FeltPluginChannel<?> felt$setupChannel(Identifier channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        return FeltFabricMessaging.setupChannel(this, channel, incoming, outgoing);
    }
}
