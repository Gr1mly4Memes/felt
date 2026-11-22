package gr1mly4memes.felt.mixin.core.world.level.redstone;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NeighborUpdater.class)
public interface NeighborUpdaterMixin {

    @Decorate(method = "executeUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;handleNeighborChanged(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;Z)V"))
    private static void felt$blockPhysicsEvent(BlockState instance, Level level, BlockPos pos, Block block, Orientation orientation, boolean movedByPiston) throws Throwable {
        if (((WorldBridge) level).bridge$isPopulating()) {
            DecorationOps.cancel().invoke();
            return;
        }
        var cworld = level.bridge$getWorld();
        if (cworld != null) {
            BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at(level, pos), CraftBlockData.createData(instance));
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                DecorationOps.cancel().invoke();
                return;
            }
        }
        DecorationOps.callsite().invoke(instance, level, pos, block, orientation, movedByPiston);
    }

    @Decorate(method = "executeUpdate", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
    private static void felt$setLastPhysicsProblem(Level level, BlockState instance, BlockPos pos, @Local(ordinal = -1) Throwable t) {
        if (t instanceof StackOverflowError) {
            ((WorldBridge) level).bridge$setLastPhysicsProblem(pos.immutable());
        }
    }
}
