package gr1mly4memes.felt.mixin.core.world.item.component;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import gr1mly4memes.felt.bridge.core.world.item.ItemStackBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CustomData.class)
public abstract class CustomDataMixin {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;withAlternative(Lcom/mojang/serialization/Codec;Lcom/mojang/serialization/Codec;)Lcom/mojang/serialization/Codec;", remap = false), require = 1, allow = 1)
    private static Codec<CompoundTag> felt$compoundCodec(Codec<CompoundTag> original) {
        return ItemStackBridge.customDataCodec();
    }
}
