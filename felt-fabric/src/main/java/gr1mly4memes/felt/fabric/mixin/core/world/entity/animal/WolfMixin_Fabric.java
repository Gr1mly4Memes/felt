package gr1mly4memes.felt.fabric.mixin.core.world.entity.animal;

import gr1mly4memes.felt.common.mixin.core.world.entity.TamableAnimalMixin;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Wolf.class)
public abstract class WolfMixin_Fabric extends TamableAnimalMixin {
}
