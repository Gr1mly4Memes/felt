package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.mod.util.FeltCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HopperBlock.class)
public class HopperBlockMixin {

    private transient BlockEntity felt$oldTicking;

    @Redirect(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)V"))
    private void felt$captureHopper(Level level, BlockPos blockPos, BlockState blockState, Entity entity, HopperBlockEntity blockEntity,
                                        BlockState state, Level originalLevel, BlockPos pos, Entity originalEntity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        felt$oldTicking = FeltCaptures.getTickingBlockEntity();
        FeltCaptures.captureTickingBlockEntity(blockEntity);
        try {
            HopperBlockEntity.entityInside(level, blockPos, blockState, entity, blockEntity);
        } finally {
            FeltCaptures.captureTickingBlockEntity(felt$oldTicking);
            felt$oldTicking = null;
        }
    }
}
