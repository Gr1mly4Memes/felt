package gr1mly4memes.felt.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gr1mly4memes.felt.bridge.core.world.level.block.entity.SculkSensorRangeBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"net.minecraft.world.level.block.entity.SculkSensorBlockEntity$VibrationUser", "net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity$VibrationUser"})
public abstract class SculkSensorVibrationRangeMixin {
    @Unique private SculkSensorBlockEntity felt$sensor;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$captureSensor(@Coerce Object sensor, BlockPos pos, CallbackInfo ci) {
        this.felt$sensor = (SculkSensorBlockEntity)sensor;
    }
    @ModifyReturnValue(method = "getListenerRadius", at = @At("RETURN"))
    private int felt$rangeOverride(int defaultRange) {
        var range = ((SculkSensorRangeBridge)this.felt$sensor).bridge$listenerRange();
        return range == null ? defaultRange : range;
    }
}
