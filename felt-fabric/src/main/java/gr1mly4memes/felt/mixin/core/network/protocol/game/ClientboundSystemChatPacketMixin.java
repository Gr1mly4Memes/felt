package gr1mly4memes.felt.mixin.core.network.protocol.game;

import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientboundSystemChatPacket.class)
public class ClientboundSystemChatPacketMixin {

    @ShadowConstructor
    public void felt$constructor(Component content, boolean overlay) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void felt$constructor(BaseComponent[] content, boolean overlay) {
        felt$constructor(CraftChatMessage.bungeeToVanilla(content), overlay);
    }
}
