package gr1mly4memes.felt.mixin.core.network.protocol.common.custom;

import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import gr1mly4memes.felt.mod.plugin.messaging.RawPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DiscardedPayload.class)
public abstract class DiscardedPayloadMixin implements RawPayload {

    @ShadowConstructor
    public abstract void felt$constructor(Identifier rl);

    @CreateConstructor
    public void felt$constructor(Identifier rl, ByteBuf data) {
        felt$constructor(rl);
        this.data = data.copy();
    }

    @Unique
    private ByteBuf data;

    @Unique
    public ByteBuf data() {
        return data;
    }

    @Unique
    @Override
    public ByteBuf felt$getRawData() {
        return data();
    }

    @Unique
    @Override
    public void felt$setData(ByteBuf data) {
        this.data = data;
    }

    @Inject(method = "codec", at = @At("HEAD"), cancellable = true)
    private static<T extends FriendlyByteBuf> void felt$interceptCodec(Identifier location, int i, CallbackInfoReturnable<StreamCodec<T, CustomPacketPayload>> cir) {
        cir.setReturnValue(RawPayload.discardedCodec(location, i));
    }
}
