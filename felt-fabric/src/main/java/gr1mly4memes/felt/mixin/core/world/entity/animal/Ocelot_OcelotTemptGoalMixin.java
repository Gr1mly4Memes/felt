package gr1mly4memes.felt.mixin.core.world.entity.animal;

import gr1mly4memes.felt.bridge.core.world.entity.ai.goal.TemptGoalBridge;
import net.minecraft.world.entity.animal.feline.Ocelot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.entity.animal.feline.Ocelot$OcelotTemptGoal")
public abstract class Ocelot_OcelotTemptGoalMixin {

    @Shadow @Final private Ocelot ocelot;

    @Overwrite
    protected boolean canScare() {
        // Native Ocelot policy: configured flag AND trusting gate.
        return ((TemptGoalBridge) (Object) this).felt$baseCanScare() && !this.ocelot.isTrusting();
    }
}
