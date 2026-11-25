package gr1mly4memes.felt.mixin.core.world.level.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DefaultRedstoneWireEvaluator.class)
public interface DefaultRedstoneWireEvaluatorAccessor {
    @Invoker("calculateTargetStrength")
    int felt$calculateTargetStrength(Level level, BlockPos pos);
}
