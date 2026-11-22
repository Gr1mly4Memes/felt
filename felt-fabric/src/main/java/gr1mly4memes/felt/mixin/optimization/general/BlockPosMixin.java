package gr1mly4memes.felt.mixin.optimization.general;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Paper - Perf: Optimize BlockPos. Directional accessors allocate through
 * {@code relative(Direction)} and re-dispatch on the enum; inline the cardinal cases
 * and avoid the extra {@code immutable()} round-trip for zero steps.
 */
@Mixin(BlockPos.class)
public abstract class BlockPosMixin {

    @Shadow public abstract BlockPos immutable();

    @Unique
    private Vec3i felt$self() {
        return (Vec3i) (Object) this;
    }

    @Inject(method = "above()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$above(CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(new BlockPos(self.getX(), self.getY() + 1, self.getZ()));
    }

    @Inject(method = "above(I)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$aboveSteps(int steps, CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(steps == 0 ? this.immutable() : new BlockPos(self.getX(), self.getY() + steps, self.getZ()));
    }

    @Inject(method = "below()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$below(CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(new BlockPos(self.getX(), self.getY() - 1, self.getZ()));
    }

    @Inject(method = "below(I)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$belowSteps(int steps, CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(steps == 0 ? this.immutable() : new BlockPos(self.getX(), self.getY() - steps, self.getZ()));
    }

    @Inject(method = "north()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$north(CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(new BlockPos(self.getX(), self.getY(), self.getZ() - 1));
    }

    @Inject(method = "north(I)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$northSteps(int steps, CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(steps == 0 ? this.immutable() : new BlockPos(self.getX(), self.getY(), self.getZ() - steps));
    }

    @Inject(method = "south()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$south(CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(new BlockPos(self.getX(), self.getY(), self.getZ() + 1));
    }

    @Inject(method = "south(I)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$southSteps(int steps, CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(steps == 0 ? this.immutable() : new BlockPos(self.getX(), self.getY(), self.getZ() + steps));
    }

    @Inject(method = "west()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$west(CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(new BlockPos(self.getX() - 1, self.getY(), self.getZ()));
    }

    @Inject(method = "west(I)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$westSteps(int steps, CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(steps == 0 ? this.immutable() : new BlockPos(self.getX() - steps, self.getY(), self.getZ()));
    }

    @Inject(method = "east()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$east(CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(new BlockPos(self.getX() + 1, self.getY(), self.getZ()));
    }

    @Inject(method = "east(I)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$eastSteps(int steps, CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(steps == 0 ? this.immutable() : new BlockPos(self.getX() + steps, self.getY(), self.getZ()));
    }

    @Inject(method = "relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void felt$relative(Direction direction, CallbackInfoReturnable<BlockPos> cir) {
        Vec3i self = this.felt$self();
        cir.setReturnValue(switch (direction) {
            case UP -> new BlockPos(self.getX(), self.getY() + 1, self.getZ());
            case DOWN -> new BlockPos(self.getX(), self.getY() - 1, self.getZ());
            case NORTH -> new BlockPos(self.getX(), self.getY(), self.getZ() - 1);
            case SOUTH -> new BlockPos(self.getX(), self.getY(), self.getZ() + 1);
            case WEST -> new BlockPos(self.getX() - 1, self.getY(), self.getZ());
            case EAST -> new BlockPos(self.getX() + 1, self.getY(), self.getZ());
            default -> new BlockPos(self.getX() + direction.getStepX(), self.getY() + direction.getStepY(), self.getZ() + direction.getStepZ());
        });
    }
}
