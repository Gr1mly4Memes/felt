package gr1mly4memes.felt.mixin.core.server.network.config;

import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.server.network.config.ServerCodeOfConductConfigurationTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerCodeOfConductConfigurationTask.class)
public abstract class ServerCodeOfConductConfigurationTaskMixin {

    @Decorate(method = "start(Ljava/util/function/Consumer;)V", require = 1, expect = 1, allow = 1,
        at = @At(value = "NEW", target = "(Ljava/lang/String;)Lnet/minecraft/network/protocol/configuration/ClientboundCodeOfConductPacket;"))
    private ClientboundCodeOfConductPacket felt$skipEmptyCodeOfConduct(String codeOfConduct) throws Throwable {
        if (codeOfConduct == null) {
            return (ClientboundCodeOfConductPacket) DecorationOps.cancel().invoke();
        }
        return (ClientboundCodeOfConductPacket) DecorationOps.callsite().invoke(codeOfConduct);
    }
}
