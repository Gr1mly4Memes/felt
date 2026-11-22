package gr1mly4memes.felt.mixin.core.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EatBlockGoal.class)
public class EatBlockGoalMixin {

    @Shadow @Final private Mob mob;

    private transient BlockPos felt$pos;

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    public void felt$capturePos1(CallbackInfo ci, BlockPos pos) {
        felt$pos = pos;
    }

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
    public void felt$capturePos2(CallbackInfo ci, BlockPos pos) {
        felt$pos = pos.below();
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    public void felt$entityChangeBlock(CallbackInfo ci) {
        var result = CraftEventFactory.callEntityChangeBlockEvent(this.mob, felt$pos, Blocks.AIR.defaultBlockState(), false);
        felt$pos = null;
        if (!result) {
            ci.cancel();
        }
    }
}
