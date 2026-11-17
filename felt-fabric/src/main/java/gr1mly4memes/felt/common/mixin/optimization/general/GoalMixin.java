package gr1mly4memes.felt.common.mixin.optimization.general;

import gr1mly4memes.felt.i18n.FeltConfig;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Goal.class)
public class GoalMixin {

    @ModifyConstant(method = "reducedTickDelay", constant = @Constant(intValue = 2))
    private static int arclight$goalUpdateInterval(int orig) {
        return FeltConfig.spec().getOptimization().getGoalSelectorInterval();
    }
}
