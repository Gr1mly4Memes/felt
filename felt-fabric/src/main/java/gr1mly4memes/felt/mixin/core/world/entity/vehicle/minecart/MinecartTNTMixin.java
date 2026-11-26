package gr1mly4memes.felt.mixin.core.world.entity.vehicle.minecart;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import gr1mly4memes.felt.bridge.core.world.entity.vehicle.MinecartTNTBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartTNT.class)
public abstract class MinecartTNTMixin extends AbstractMinecartMixin implements MinecartTNTBridge {

    @Shadow public int fuse;

    public boolean isIncendiary;
    @Unique private Integer felt$requestedFuse;

    @Override
    public boolean bridge$isIncendiary() {
        return this.isIncendiary;
    }

    @Override
    public void bridge$setIncendiary(boolean value) {
        this.isIncendiary = value;
    }

    @Decorate(method = "destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/MinecartTNT;primeFuse(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void felt$primeWithShortFuse(MinecartTNT cart, DamageSource source) throws Throwable {
        Integer previous = this.felt$requestedFuse;
        this.felt$requestedFuse = cart.getRandom().nextInt(20) + cart.getRandom().nextInt(20);
        try {
            DecorationOps.callsite().invoke(cart, source);
        } finally {
            this.felt$requestedFuse = previous;
        }
        // The native branch would now overwrite the fuse chosen by the event.
        DecorationOps.cancel().invoke();
    }

    @WrapOperation(method = "primeFuse", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/world/entity/vehicle/minecart/MinecartTNT;fuse:I"))
    private void felt$notifyIgnition(MinecartTNT cart, int ticks, Operation<Void> original, @Cancellable CallbackInfo ci) {
        Integer requestedFuse = this.felt$requestedFuse;
        this.felt$requestedFuse = null;
        if (requestedFuse != null) {
            ticks = requestedFuse;
        }
        if (cart.level() instanceof ServerLevel) {
            ticks = CraftEventFactory.callEntityIgniteEvent(cart, ticks);
            if (ticks == PrimedTnt.NO_FUSE) {
                ci.cancel();
                return;
            }
        }
        original.call(cart, ticks);
    }

    @Decorate(method = "explode(Lnet/minecraft/world/damagesource/DamageSource;D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void felt$explode(ServerLevel level, Entity entity, DamageSource source, ExplosionDamageCalculator calculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction) throws Throwable {
        var event = new ExplosionPrimeEvent(this.getBukkitEntity(), radius, this.isIncendiary);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.fuse = -1;
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.callsite().invoke(level, entity, source, calculator, x, y, z, event.getRadius(), event.getFire(), interaction);
    }

    @Inject(method = "explode(Lnet/minecraft/world/damagesource/DamageSource;D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/MinecartTNT;discard()V"))
    private void felt$explodeCause(DamageSource damageSource, double d, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
    }
}
