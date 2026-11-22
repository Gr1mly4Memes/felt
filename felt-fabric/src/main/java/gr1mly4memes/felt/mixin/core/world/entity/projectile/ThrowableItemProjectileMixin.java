package gr1mly4memes.felt.mixin.core.world.entity.projectile;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ThrowableItemProjectile.class)
public abstract class ThrowableItemProjectileMixin extends ThrowableProjectileMixin {

    // @formatter:off
    @Shadow protected abstract Item getDefaultItem();
    // @formatter:on

    public Item getDefaultItemPublic() {
        return this.getDefaultItem();
    }
}
