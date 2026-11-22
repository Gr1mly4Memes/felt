package gr1mly4memes.felt.mixin.core.world.entity;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Creeper.class)
public interface CreeperDeathAccessor {
    @Accessor("droppedSkulls")
    void felt$droppedSkulls(boolean dropped);
}
