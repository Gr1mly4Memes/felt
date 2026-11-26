package gr1mly4memes.felt.mixin.core.world.entity.vehicle.minecart;

import gr1mly4memes.felt.mixin.core.world.entity.vehicle.VehicleEntityMixin;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.world.entity.vehicle.AbstractMinecartBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends VehicleEntityMixin implements AbstractMinecartBridge {

    public boolean slowWhenEmpty = true;
    private double derailedX = 0.5;
    private double derailedY = 0.5;
    private double derailedZ = 0.5;
    private double flyingX = 0.95;
    private double flyingY = 0.95;
    private double flyingZ = 0.95;
    public Double maxSpeed;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void felt$init(EntityType<?> type, Level worldIn, CallbackInfo ci) {
        slowWhenEmpty = true;
        derailedX = 0.5;
        derailedY = 0.5;
        derailedZ = 0.5;
        flyingX = 0.95;
        flyingY = 0.95;
        flyingZ = 0.95;
        maxSpeed = null;
    }

    @Unique
    private transient Location felt$prevLocation;

    @Decorate(method = "tick", inject = true, at = @At("HEAD"))
    private void felt$storePreviousLocation() {
        this.felt$prevLocation = new Location(null, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/MinecartBehavior;tick()V", shift = At.Shift.AFTER))
    private void felt$vehicleUpdateEvent(CallbackInfo ci) {
        org.bukkit.World bworld = this.level().bridge$getWorld();
        Location to = new Location(bworld, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        Bukkit.getPluginManager().callEvent(new VehicleUpdateEvent(vehicle));
        Location from = this.felt$prevLocation;
        if (from != null) {
            from.setWorld(bworld);
            if (!from.equals(to)) {
                Bukkit.getPluginManager().callEvent(new VehicleMoveEvent(vehicle, from, to));
            }
        }
    }

    @org.spongepowered.asm.mixin.Shadow
    protected abstract double getMaxSpeed(ServerLevel level);

    @Override
    public Double bridge$getMaxSpeedOverride() {
        return this.maxSpeed;
    }

    @Decorate(method = "comeOffTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private Vec3 felt$derailedVelocity(Vec3 movement, double factor) throws Throwable {
        Vec3 result = (Vec3) DecorationOps.callsite().invoke(movement, factor);
        if (this.derailedX == 0.5 && this.derailedY == 0.5 && this.derailedZ == 0.5) {
            return result;
        }
        return result.multiply(this.derailedX / 0.5, this.derailedY / 0.5, this.derailedZ / 0.5);
    }

    @Decorate(method = "comeOffTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal = 1))
    private Vec3 felt$flyingVelocity(Vec3 movement, double factor) throws Throwable {
        Vec3 result = (Vec3) DecorationOps.callsite().invoke(movement, factor);
        if (this.flyingX == 0.95 && this.flyingY == 0.95 && this.flyingZ == 0.95) {
            return result;
        }
        // Retain peer drag (for example Railcraft) and apply Bukkit's per-axis modifier relative to vanilla.
        return result.multiply(this.flyingX / 0.95, this.flyingY / 0.95, this.flyingZ / 0.95);
    }

    @Override
    public boolean bridge$isSlowWhenEmpty() {
        return this.slowWhenEmpty;
    }

    @Inject(method = "push", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;hasPassenger(Lnet/minecraft/world/entity/Entity;)Z"))
    private void felt$vehicleCollide(Entity entityIn, CallbackInfo ci) {
        if (!this.hasPassenger(entityIn)) {
            var cart = (Vehicle) this.getBukkitEntity();
            var other = ((EntityBridge) entityIn).bridge$getBukkitEntity();
            if (io.papermc.paper.event.entity.EntityCollideWithEntityEvent.getHandlerList().hasListeners()
                && !new io.papermc.paper.event.entity.EntityCollideWithEntityEvent(cart, other).callEvent()) {
                ci.cancel();
                return;
            }
            VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(cart, other);
            Bukkit.getPluginManager().callEvent(collisionEvent);
            if (collisionEvent.isCancelled()) {
                ci.cancel();
            }
        }
    }

    public Vector getFlyingVelocityMod() {
        return new Vector(flyingX, flyingY, flyingZ);
    }

    public void setFlyingVelocityMod(Vector flying) {
        flyingX = flying.getX();
        flyingY = flying.getY();
        flyingZ = flying.getZ();
    }

    public Vector getDerailedVelocityMod() {
        return new Vector(derailedX, derailedY, derailedZ);
    }

    public void setDerailedVelocityMod(Vector derailed) {
        derailedX = derailed.getX();
        derailedY = derailed.getY();
        derailedZ = derailed.getZ();
    }
}
