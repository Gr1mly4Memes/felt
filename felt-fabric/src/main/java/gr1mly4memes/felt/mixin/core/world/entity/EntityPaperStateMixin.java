package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import java.util.UUID;
import net.kyori.adventure.util.TriState;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPaperStateMixin implements PaperEntityStateBridge, gr1mly4memes.felt.bridge.core.world.LootableDataBridge {
    @Unique private com.destroystokyo.paper.loottable.PaperLootableInventoryData felt$lootableData;
    @Override public com.destroystokyo.paper.loottable.PaperLootableInventoryData bridge$lootableData() {
        if (this.felt$lootableData == null) this.felt$lootableData = new com.destroystokyo.paper.loottable.PaperLootableInventoryData();
        return this.felt$lootableData;
    }
    @Shadow private boolean hasVisualFire;
    @Shadow protected abstract void setSharedFlag(int flag, boolean value);
    @Unique private boolean felt$freezeLocked;
    @Unique private boolean felt$burnInDay = true;
    @Unique private TriState felt$visualFire = TriState.NOT_SET;
    @Unique private Vec3 felt$origin;
    @Unique private UUID felt$originWorld;
    @Unique private boolean felt$fromSpawner;
    @Unique private TriState felt$despawnInPeaceful = TriState.NOT_SET;
    @Unique private TriState felt$frictionState = TriState.NOT_SET;
    @Unique private boolean felt$canMobPickup = true;
    @Override public TriState bridge$frictionState() { return this.felt$frictionState; }
    @Override public void bridge$setFrictionState(TriState state) { this.felt$frictionState = java.util.Objects.requireNonNull(state); }
    @Override public boolean bridge$canMobPickup() { return this.felt$canMobPickup; }
    @Override public void bridge$setCanMobPickup(boolean pickup) { this.felt$canMobPickup = pickup; }
    @Override public TriState bridge$despawnInPeaceful() { return this.felt$despawnInPeaceful; }
    @Override public void bridge$setDespawnInPeaceful(TriState state) { this.felt$despawnInPeaceful = java.util.Objects.requireNonNull(state); }
    @Override public boolean bridge$freezeLocked() { return this.felt$freezeLocked; }
    @Override public void bridge$setFreezeLocked(boolean locked) { this.felt$freezeLocked = locked; }
    @Override public boolean bridge$burnInDay() { return this.felt$burnInDay; }
    @Override public void bridge$setBurnInDay(boolean burn) { this.felt$burnInDay = burn; }
    @Override public TriState bridge$visualFire() { return this.felt$visualFire; }
    @Override public void bridge$setVisualFire(TriState state) {
        this.felt$visualFire = java.util.Objects.requireNonNull(state);
        this.hasVisualFire = state == TriState.TRUE;
    }
    @Override public boolean bridge$fromSpawner() { return this.felt$fromSpawner; }
    @Override public void bridge$markFromSpawner() { this.felt$fromSpawner = true; }
    @Override public Location bridge$origin() {
        if (this.felt$origin == null) return null;
        Entity entity = (Entity)(Object)this;
        var world = this.felt$originWorld == null ? ((WorldBridge)entity.level()).bridge$getWorld() : org.bukkit.Bukkit.getWorld(this.felt$originWorld);
        return new Location(world, this.felt$origin.x, this.felt$origin.y, this.felt$origin.z);
    }
    @Override public void bridge$captureOrigin() {
        Entity entity = (Entity)(Object)this;
        if (this.felt$origin == null) this.felt$origin = entity.position();
        if (this.felt$originWorld == null) this.felt$originWorld = ((WorldBridge)entity.level()).bridge$getWorld().getUID();
    }
    @Inject(method = "setSharedFlagOnFire", at = @At("HEAD"), cancellable = true)
    private void felt$visualFireFlag(boolean burning, CallbackInfo ci) {
        this.setSharedFlag(0, this.felt$visualFire.toBooleanOrElse(burning));
        ci.cancel();
    }
    @Inject(method = "clearFreeze", at = @At("HEAD"), cancellable = true)
    private void felt$keepLockedFreezeTicks(CallbackInfo ci) {
        if (this.felt$freezeLocked) ci.cancel();
    }
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void felt$savePaperState(ValueOutput output, CallbackInfo ci) {
        if (this.felt$freezeLocked) output.putBoolean("Paper.FreezeLock", true);
        if (this.felt$visualFire != TriState.NOT_SET) output.putString("Paper.FireOverride", this.felt$visualFire.name());
        if (this.felt$fromSpawner) output.putBoolean("Paper.FromMobSpawner", true);
        if (this.felt$despawnInPeaceful != TriState.NOT_SET) output.putString("Paper.DespawnInPeacefulOverride", this.felt$despawnInPeaceful.name());
        if (this.felt$frictionState != TriState.NOT_SET) output.putString("Paper.FrictionState", this.felt$frictionState.name());
        if (this.felt$origin != null) {
            output.store("Paper.Origin", Vec3.CODEC, this.felt$origin);
            if (this.felt$originWorld != null) output.store("Paper.OriginWorld", UUIDUtil.CODEC, this.felt$originWorld);
        }
        if ((Object)this instanceof Zombie || (Object)this instanceof AbstractSkeleton || (Object)this instanceof Phantom)
            output.putBoolean("Paper.ShouldBurnInDay", this.felt$burnInDay);
    }
    @Inject(method = "load", at = @At("RETURN"))
    private void felt$loadPaperState(ValueInput input, CallbackInfo ci) {
        this.felt$freezeLocked = input.getBooleanOr("Paper.FreezeLock", false);
        this.felt$burnInDay = input.getBooleanOr("Paper.ShouldBurnInDay", true);
        this.felt$fromSpawner = input.getBooleanOr("Paper.FromMobSpawner", false);
        this.felt$despawnInPeaceful = input.read("Paper.DespawnInPeacefulOverride", io.papermc.paper.util.PaperCodecs.TRI_STATE_CODEC).orElse(TriState.NOT_SET);
        this.felt$frictionState = TriState.NOT_SET;
        input.getString("Paper.FrictionState").ifPresent(value -> {
            try { this.felt$frictionState = TriState.valueOf(value); }
            catch (IllegalArgumentException invalid) { org.slf4j.LoggerFactory.getLogger(Entity.class).error("Unknown friction state {} for {}", value, this); }
        });
        this.felt$origin = input.read("Paper.Origin", Vec3.CODEC).orElse(null);
        this.felt$originWorld = input.read("Paper.OriginWorld", UUIDUtil.CODEC).orElse(null);
        this.felt$visualFire = this.hasVisualFire ? TriState.TRUE : TriState.NOT_SET;
        input.getString("Paper.FireOverride").ifPresent(value -> {
            try { this.bridge$setVisualFire(TriState.valueOf(value)); }
            catch (IllegalArgumentException invalid) { org.slf4j.LoggerFactory.getLogger(Entity.class).error("Unknown fire override {} for {}", value, this); }
        });
    }
}
