package gr1mly4memes.felt.common.mixin.core;

import gr1mly4memes.felt.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.ChatFormatting;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatFormatting.class)
public abstract class ChatFormattingMixin {
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static ChatFormatting getByHexValue(int color) {
        for (ChatFormatting formatting : ChatFormatting.values()) {
            Integer formattingColor = formatting.getColor();
            if (formattingColor != null && formattingColor == color) {
                return formatting;
            }
        }
        return null;
    }
}
