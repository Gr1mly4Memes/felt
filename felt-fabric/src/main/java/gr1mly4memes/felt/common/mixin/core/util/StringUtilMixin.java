package gr1mly4memes.felt.common.mixin.core.util;

import gr1mly4memes.felt.i18n.FeltConfig;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringUtil.class)
public class StringUtilMixin {

    @Unique
    private static boolean arclight$validUsernameCheck(String name) {
        var regex = FeltConfig.spec().getCompat().getValidUsernameRegex();
        return !regex.isBlank() && name.matches(regex);
    }

    @Inject(method = "isValidPlayerName", cancellable = true, at = @At("HEAD"))
    private static void arclight$checkUsername(String name, CallbackInfoReturnable<Boolean> cir) {
        if (arclight$validUsernameCheck(name)) {
            cir.setReturnValue(true);
        }
    }
}
