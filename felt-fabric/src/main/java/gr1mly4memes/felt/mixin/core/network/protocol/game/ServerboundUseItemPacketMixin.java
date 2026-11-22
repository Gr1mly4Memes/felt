package gr1mly4memes.felt.mixin.core.network.protocol.game;

import gr1mly4memes.felt.bridge.core.network.play.TimestampedPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundUseItemPacket.class)
public class ServerboundUseItemPacketMixin implements TimestampedPacket {

    public long timestamp;

    @Inject(method = "<init>(Lnet/minecraft/world/InteractionHand;IFF)V", at = @At("RETURN"))
    private void felt$read(CallbackInfo ci) {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public long bridge$timestamp() {
        return timestamp;
    }
}
