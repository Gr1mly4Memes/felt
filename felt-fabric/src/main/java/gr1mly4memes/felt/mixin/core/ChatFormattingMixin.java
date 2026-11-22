package gr1mly4memes.felt.mixin.core;

import gr1mly4memes.felt.mod.mixins.annotation.TransformAccess;
import net.minecraft.ChatFormatting;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatFormatting.class)
public abstract class ChatFormattingMixin {
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static ChatFormatting getByHexValue(int color) {
        for (ChatFormatting formatting : ChatFormatting.values()) {
            net.minecraft.network.chat.TextColor textColor = net.minecraft.network.chat.TextColor.fromLegacyFormat(formatting);
            Integer formattingColor = textColor == null ? null : textColor.getValue();
            if (formattingColor != null && formattingColor == color) {
                return formatting;
            }
        }
        return null;
    }
}
