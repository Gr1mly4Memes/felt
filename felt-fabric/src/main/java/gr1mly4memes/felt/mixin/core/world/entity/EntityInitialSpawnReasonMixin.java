package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.entity.InitialSpawnReasonBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityInitialSpawnReasonMixin implements InitialSpawnReasonBridge {
    @Unique private SpawnReason felt$initialSpawnReason;
    @Override public SpawnReason bridge$initialSpawnReason() { return this.felt$initialSpawnReason; }
    @Override public void bridge$setInitialSpawnReason(SpawnReason reason) {
        if (this.felt$initialSpawnReason == null) this.felt$initialSpawnReason = reason;
    }
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void felt$saveSpawnReason(ValueOutput output, CallbackInfo ci) {
        if (this.felt$initialSpawnReason != null) output.putString("Paper.SpawnReason", this.felt$initialSpawnReason.name());
    }
    @Inject(method = "load", at = @At("RETURN"))
    private void felt$loadSpawnReason(ValueInput input, CallbackInfo ci) {
        this.felt$initialSpawnReason = null;
        input.getString("Paper.SpawnReason").ifPresent(name -> {
            try { this.felt$initialSpawnReason = SpawnReason.valueOf(name); }
            catch (IllegalArgumentException invalid) {
                org.slf4j.LoggerFactory.getLogger(Entity.class).error("Unknown SpawnReason {} for {}", name, this);
            }
        });
        if (this.felt$initialSpawnReason == null && input.getBooleanOr("Paper.FromMobSpawner", false)) this.felt$initialSpawnReason = SpawnReason.SPAWNER;
        if (this.felt$initialSpawnReason == null && (Object)this instanceof Mob mob
            && (mob instanceof Animal || mob instanceof AbstractFish) && !mob.removeWhenFarAway(0.0)
            && !input.getBooleanOr("PersistenceRequired", false)) this.felt$initialSpawnReason = SpawnReason.NATURAL;
        if (this.felt$initialSpawnReason == null) this.felt$initialSpawnReason = SpawnReason.DEFAULT;
    }
}
