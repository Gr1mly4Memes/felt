package gr1mly4memes.felt.mixin.optimization.general;

import gr1mly4memes.felt.mod.config.FeltConfig;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Goal.class)
public class GoalMixin {

    @ModifyConstant(method = "reducedTickDelay", constant = @Constant(intValue = 2))
    private static int felt$goalUpdateInterval(int orig) {
        return FeltConfig.goalSelectorUpdateInterval;
    }
}
