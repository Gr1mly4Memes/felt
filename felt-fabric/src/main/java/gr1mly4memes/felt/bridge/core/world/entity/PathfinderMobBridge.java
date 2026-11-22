package gr1mly4memes.felt.bridge.core.world.entity;

import net.minecraft.core.BlockPos;

public interface PathfinderMobBridge {
    BlockPos bridge$getMovingTarget();
    void bridge$setMovingTarget(BlockPos pos);
}
