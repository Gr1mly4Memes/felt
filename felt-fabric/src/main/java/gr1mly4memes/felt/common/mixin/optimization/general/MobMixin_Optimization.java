package gr1mly4memes.felt.common.mixin.optimization.general;

import gr1mly4memes.felt.i18n.FeltConfig;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Mob.class)
public class MobMixin_Optimization {

    @ModifyConstant(method = "serverAiStep", constant = @Constant(intValue = 2))
    private int arclight$goalUpdateInterval(int orig) {
        return FeltConfig.spec().getOptimization().getGoalSelectorInterval();
    }
}
