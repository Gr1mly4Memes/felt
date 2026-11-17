package gr1mly4memes.felt.common.mod.plugin.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FeltRawPayload implements RawPayload {

    public static final Map<Identifier, CustomPacketPayload.Type<FeltRawPayload>> REGISTRY = new HashMap<>();

    public static CustomPacketPayload.Type<FeltRawPayload> getType(Identifier channel) {
        return REGISTRY.computeIfAbsent(channel, CustomPacketPayload.Type::new);
    }

    private final Type<FeltRawPayload> type;
    private ByteBuf data;

    public FeltRawPayload(CustomPacketPayload.Type<FeltRawPayload> type, ByteBuf raw) {
        Objects.requireNonNull(type, "type cannot be null");
        this.type = type;
        this.data = raw;
    }

    public FeltRawPayload(CustomPacketPayload.Type<FeltRawPayload> type, byte[] raw) {
        this(type, Unpooled.wrappedBuffer(raw));
    }

    @Override
    public Type<FeltRawPayload> type() {
        return type;
    }

    @Override
    public ByteBuf arclight$getRawData() {
        return data;
    }

    @Override
    public void arclight$setData(ByteBuf data) {
        this.data = data;
    }
}
