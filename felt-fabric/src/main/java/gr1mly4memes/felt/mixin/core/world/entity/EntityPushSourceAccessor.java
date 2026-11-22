package gr1mly4memes.felt.mixin.core.world.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityPushSourceAccessor {
    @Accessor(value = "felt$attackPushSource", remap = false)
    Entity felt$getAttackPushSource();

    @Accessor(value = "felt$attackPushSource", remap = false)
    void felt$setAttackPushSource(Entity source);
}
