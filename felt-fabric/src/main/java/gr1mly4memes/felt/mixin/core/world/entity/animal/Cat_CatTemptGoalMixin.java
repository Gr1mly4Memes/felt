package gr1mly4memes.felt.mixin.core.world.entity.animal;

import gr1mly4memes.felt.bridge.core.world.entity.ai.goal.TemptGoalBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.jspecify.annotations.Nullable;

@Mixin(targets = "net.minecraft.world.entity.animal.feline.Cat$CatTemptGoal")
public abstract class Cat_CatTemptGoalMixin {

    @Shadow @Final private Cat cat;
    @Shadow @Nullable private Player selectedPlayer;

    @Unique private @Nullable LivingEntity felt$selectedLiving;

    @Overwrite
    public void tick() {
        TemptGoalBridge goal = (TemptGoalBridge) (Object) this;
        goal.felt$runTemptTick();
        LivingEntity target = goal.felt$getEffectiveTarget();

        // Preserve Cat's exact 600/500 adjusted-delay draw order and else-if behavior.
        if (this.felt$selectedLiving == null && this.cat.getRandom().nextInt(goal.felt$adjustedTickDelay(600)) == 0) {
            this.felt$selectedLiving = target;
            this.selectedPlayer = target instanceof Player ? (Player) target : null;
        } else if (this.cat.getRandom().nextInt(goal.felt$adjustedTickDelay(500)) == 0) {
            this.felt$selectedLiving = null;
            this.selectedPlayer = null;
        }
    }

    @Overwrite
    protected boolean canScare() {
        TemptGoalBridge goal = (TemptGoalBridge) (Object) this;
        return this.felt$selectedLiving != null && this.felt$selectedLiving.equals(goal.felt$getEffectiveTarget())
            ? false
            : goal.felt$baseCanScare();
    }
}
