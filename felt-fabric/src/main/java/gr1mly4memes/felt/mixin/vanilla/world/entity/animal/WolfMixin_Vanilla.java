package gr1mly4memes.felt.mixin.vanilla.world.entity.animal;

import gr1mly4memes.felt.mixin.core.world.entity.TamableAnimalMixin;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Wolf.class)
public abstract class WolfMixin_Vanilla extends TamableAnimalMixin {
}
