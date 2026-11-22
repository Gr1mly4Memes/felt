package gr1mly4memes.felt.mixin.core.world.entity.animal;

import gr1mly4memes.felt.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Rabbit.class)
public abstract class RabbitMixin extends AnimalMixin {

    // @formatter:off
    @Shadow public abstract void setSpeedModifier(double newSpeed);
    // @formatter:on

    public void initializePathFinderGoals() {
        this.setSpeedModifier(0.0D);
    }
}
