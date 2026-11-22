package gr1mly4memes.felt.mixin.core.network.protocol.game;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface ClientboundPlayerInfoUpdatePacketAccessor {
    @Mutable
    @Accessor("entries")
    void felt$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
