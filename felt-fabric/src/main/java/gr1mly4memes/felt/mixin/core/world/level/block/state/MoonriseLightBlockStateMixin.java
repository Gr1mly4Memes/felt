package gr1mly4memes.felt.mixin.core.world.level.block.state;

import ca.spottedleaf.moonrise.patches.starlight.blockstate.StarlightAbstractBlockState;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseLightBlockStateMixin implements StarlightAbstractBlockState {
    @Shadow private boolean canOcclude;
    @Shadow private boolean useShapeForLightOcclusion;
    @Unique private boolean felt$conditionalOpaque;
    @Inject(method = "initCache", at = @At("RETURN"))
    private void felt$lightOpacity(CallbackInfo ci) { this.felt$conditionalOpaque = this.canOcclude & this.useShapeForLightOcclusion; }
    @Override public boolean starlight$isConditionallyFullOpaque() { return this.felt$conditionalOpaque; }
}
