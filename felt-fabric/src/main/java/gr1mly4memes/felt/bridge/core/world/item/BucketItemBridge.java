package gr1mly4memes.felt.bridge.core.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface BucketItemBridge {
    @Nullable Direction felt$getDirection();
    void felt$setDirection(@Nullable Direction value);

    @Nullable BlockPos felt$getClick();
    void felt$setClick(@Nullable BlockPos value);

    @Nullable InteractionHand felt$getHand();
    void felt$setHand(@Nullable InteractionHand value);

    @Nullable ItemStack felt$getStack();
    void felt$setStack(@Nullable ItemStack value);

    @Nullable org.bukkit.inventory.ItemStack felt$getCaptureItem();
    void felt$setCaptureItem(@Nullable org.bukkit.inventory.ItemStack value);
}
