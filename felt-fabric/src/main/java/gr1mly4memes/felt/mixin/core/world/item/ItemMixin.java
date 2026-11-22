package gr1mly4memes.felt.mixin.core.world.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Item.class)
public abstract class ItemMixin {

    // @formatter:off
    @Shadow public ItemStack finishUsingItem(ItemStack arg, Level arg2, LivingEntity arg3) { return null; }
    // @formatter:on
}
