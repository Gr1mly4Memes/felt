package gr1mly4memes.felt.mixin.core.world.entity.monster;

import gr1mly4memes.felt.bridge.core.world.entity.SpawningEntityBridge;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Phantom.class)
public abstract class PhantomSpawningEntityMixin implements SpawningEntityBridge {
    @Unique private UUID felt$spawningEntity;

    @Override
    public UUID bridge$getSpawningEntity() {
        return this.felt$spawningEntity;
    }

    @Override
    public void bridge$setSpawningEntity(UUID spawningEntity) {
        this.felt$spawningEntity = spawningEntity;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void felt$saveSpawningEntity(ValueOutput output, CallbackInfo ci) {
        output.storeNullable("Paper.SpawningEntity", UUIDUtil.CODEC, this.felt$spawningEntity);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void felt$loadSpawningEntity(ValueInput input, CallbackInfo ci) {
        this.felt$spawningEntity = input.read("Paper.SpawningEntity", UUIDUtil.CODEC).orElse(null);
    }
}
