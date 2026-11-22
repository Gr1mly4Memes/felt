package gr1mly4memes.felt.mixin.optimization.general;

import gr1mly4memes.felt.mod.config.FeltConfig;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Mob.class)
public class MobMixin_Optimization {

    @ModifyConstant(method = "serverAiStep", constant = @Constant(intValue = 2))
    private int felt$goalUpdateInterval(int orig) {
        return FeltConfig.goalSelectorUpdateInterval;
    }
}
