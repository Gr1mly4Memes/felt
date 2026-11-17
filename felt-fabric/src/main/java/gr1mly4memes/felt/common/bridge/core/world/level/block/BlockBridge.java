package gr1mly4memes.felt.common.bridge.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface BlockBridge {

    default net.minecraft.util.TriState bridge$mod$canSustainPlant(BlockState soil, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction, BlockState plant) {
        return net.minecraft.util.TriState.DEFAULT;
    }

    int bridge$getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack);

    int bridge$tryDropExperience(ServerLevel worldserver, BlockPos blockposition, ItemStack itemstack, IntProvider intprovider);

    default boolean bridge$mod$onCropsGrowPre(Level level, BlockPos pos, BlockState state, boolean def) {
        return true;
    }

    default void bridge$mod$onCropsGrowPost(Level level, BlockPos pos, BlockState state) {}

    default void bridge$mod$onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {}
}
