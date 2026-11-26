package gr1mly4memes.felt.mixin.core.world.entity.decoration;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.bridge.core.world.entity.decoration.DecorationDeathSourceBridge;
import gr1mly4memes.felt.mixin.core.world.entity.EntityMixin;
import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import io.papermc.paper.event.entity.EntityBreakEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Hanging;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityMixin extends EntityMixin implements DecorationDeathSourceBridge {

    // @formatter:off
    @Shadow protected BlockPos pos;
    @Shadow public abstract BlockPos getPos();
    // @formatter:on

    @Unique private DamageSource felt$decorationDeathSource;

    @Override
    public DamageSource bridge$getDecorationDeathSource() {
        return this.felt$decorationDeathSource;
    }

    protected void felt$setDecorationDeathSource(DamageSource source) {
        this.felt$decorationDeathSource = source;
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"),
        require = 1)
    private void felt$rememberDamageSource(BlockAttachedEntity decoration, ServerLevel level, Entity attacker,
                                           Operation<Void> original, @Local(argsOnly = true) DamageSource source) {
        this.felt$decorationDeathSource = source;
        original.call(decoration, level, attacker);
    }

    @ModifyArg(method = "kill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/Entity;)V"),
        index = 1)
    private Entity felt$emitDeathFromDecoration(Entity original) {
        return this.felt$decorationDeathSource == null ? original : (Entity) (Object) this;
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V", cancellable = true, at = @At("HEAD"))
    private void felt$skipSave(ValueOutput output, CallbackInfo ci) {
        if (this.felt$saveNotIncludeAll) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;discard()V"))
    private void felt$hangingBreak(CallbackInfo ci) {
        var state = this.level().getBlockState(this.blockPosition().immutable());
        var cause = state.isAir() ? EntityBreakEvent.RemoveCause.PHYSICS : EntityBreakEvent.RemoveCause.OBSTRUCTION;
        var bukkitEntity = this.getBukkitEntity();
        boolean cancelled = bukkitEntity instanceof Hanging hanging
            && !new HangingBreakEvent(hanging, HangingBreakEvent.RemoveCause.valueOf(cause.name())).callEvent();
        var event = new EntityBreakEvent(bukkitEntity, cause);
        event.setCancelled(cancelled);
        if (!event.callEvent() || this.isRemoved()) {
            ci.cancel();
        } else {
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
        }
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"))
    private void felt$hangingBreakByAttack(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var bukkitEntity = this.getBukkitEntity();
        Entity damager = source.getDirectEntity();
        var remover = damager == null ? null : damager.bridge$getBukkitEntity();
        var cause = source.is(DamageTypeTags.IS_EXPLOSION) ? EntityBreakEvent.RemoveCause.EXPLOSION
            : remover == null ? EntityBreakEvent.RemoveCause.DEFAULT : EntityBreakEvent.RemoveCause.ENTITY;
        var damage = new org.bukkit.craftbukkit.damage.CraftDamageSource(source);
        boolean cancelled = false;
        if (bukkitEntity instanceof Hanging hanging) {
            var legacyCause = HangingBreakEvent.RemoveCause.valueOf(cause.name());
            HangingBreakEvent event = remover == null ? new HangingBreakEvent(hanging, legacyCause)
                : new HangingBreakByEntityEvent(hanging, remover, damage, legacyCause);
            cancelled = !event.callEvent();
        }
        EntityBreakEvent event = remover == null ? new EntityBreakEvent(bukkitEntity, cause)
            : new EntityBreakByEntityEvent(bukkitEntity, remover, damage, cause);
        event.setCancelled(cancelled);
        if (!event.callEvent() || this.isRemoved()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "move", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void felt$hangingBreakByMove(MoverType typeIn, Vec3 pos, CallbackInfo ci) {
        // Modded block-attached entities need not implement Bukkit Hanging.
        if (!(this.getBukkitEntity() instanceof Hanging hanging)) {
            return;
        }
        if (this.isRemoved()) {
            ci.cancel();
            return;
        }
        HangingBreakEvent event = new HangingBreakEvent(hanging, HangingBreakEvent.RemoveCause.PHYSICS);
        Bukkit.getPluginManager().callEvent(event);
        if (this.isRemoved() || event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "push", cancellable = true, at = @At("HEAD"))
    private void felt$noVelocity(double x, double y, double z, CallbackInfo ci) {
        ci.cancel();
    }

    private static double a(int i) {
        return i % 32 == 0 ? 0.5D : 0.0D;
    }
}
