package gr1mly4memes.felt.common.bridge.core.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ItemBridge {

    default int bridge$mod$onArrowLoose(ItemStack stack, Level level, Player player, int charge, boolean hasAmmo) {
        return charge;
    }

    default AbstractArrow bridge$mod$customArrow(BowItem bowItem, ItemStack weapon, ItemStack projectile, AbstractArrow arrow) {
        return arrow;
    }

    default boolean bridge$mod$onChorusFruitTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
        return false;
    }

    default void bridge$mod$onPlayerDestroyItem(Player player, @NotNull ItemStack stack, @Nullable InteractionHand hand) {}
}
