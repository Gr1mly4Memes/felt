package gr1mly4memes.felt.mixin.core.world.entity.projectile;

import gr1mly4memes.felt.bridge.core.world.entity.SpawningEntityBridge;
import gr1mly4memes.felt.mixin.core.world.entity.EntityMixin;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends EntityMixin implements SpawningEntityBridge {
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
        output.storeNullable("SpawningEntity", UUIDUtil.CODEC, this.felt$spawningEntity);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void felt$loadSpawningEntity(ValueInput input, CallbackInfo ci) {
        this.felt$spawningEntity = input.read("SpawningEntity", UUIDUtil.CODEC).orElse(null);
    }

    @Shadow public int life;
    @Shadow public int lifetime;
    @Shadow private void explode(ServerLevel level) { }

    @Override
    public void bridge$inactiveTick() {
        ++this.life;
        if (this.life > this.lifetime && this.level() instanceof ServerLevel serverLevel) {
            // The existing explode injection owns the event and removal cause.
            this.explode(serverLevel);
        }
        super.bridge$inactiveTick();
    }

    @Inject(method = "explode", cancellable = true, at = @At("HEAD"))
    private void felt$fireworksExplode(CallbackInfo ci) {
        if (!CraftEventFactory.callFireworkExplodeEvent((FireworkRocketEntity) (Object) this)) {
            ci.cancel();
        } else {
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
        }
    }
}
