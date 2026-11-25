package gr1mly4memes.felt.mixin.core.world.level.block.entity;

import gr1mly4memes.felt.bridge.core.world.level.block.entity.SculkSensorRangeBridge;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSensorBlockEntity.class)
public abstract class SculkSensorRangeMixin implements SculkSensorRangeBridge {
    @Unique private Integer felt$listenerRange;
    @Override public Integer bridge$listenerRange() { return this.felt$listenerRange; }
    @Override public void bridge$setListenerRange(int range) { this.felt$listenerRange = range; }
    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void felt$saveRange(ValueOutput output, CallbackInfo ci) {
        int defaultRange = (Object)this instanceof CalibratedSculkSensorBlockEntity ? 16 : 8;
        if (this.felt$listenerRange != null && this.felt$listenerRange != defaultRange) output.putInt("Paper.ListenerRange", this.felt$listenerRange);
    }
    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void felt$loadRange(ValueInput input, CallbackInfo ci) {
        this.felt$listenerRange = input.getInt("Paper.ListenerRange").orElse(null);
    }
}
