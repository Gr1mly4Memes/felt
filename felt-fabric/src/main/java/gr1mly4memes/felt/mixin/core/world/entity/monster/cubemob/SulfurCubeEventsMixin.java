package gr1mly4memes.felt.mixin.core.world.entity.monster.cubemob;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.bridge.core.world.damagesource.DamageSourceBridge;
import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felt.mod.server.event.FeltShearing;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityCollideWithEntityEvent;
import io.papermc.paper.event.entity.SulfurCubeSwallowItemEvent;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SulfurCubeArchetype;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SulfurCube.class)
public abstract class SulfurCubeEventsMixin {
    @Shadow private Optional<SulfurCubeArchetype.ExplosionData> explosionData;

    @Inject(method = "primeTime", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;setPermanentlyInvulnerable(Z)V"), cancellable = true)
    private void felt$igniteCube(boolean imminent, CallbackInfoReturnable<Boolean> cir, @Local(index = 4) LocalIntRef fuse) {
        int delay = CraftEventFactory.callEntityIgniteEvent((SulfurCube) (Object) this, fuse.get());
        if (delay == PrimedTnt.NO_FUSE) {
            cir.setReturnValue(false);
        } else {
            fuse.set(delay);
        }
    }

    @Inject(method = "tickFuse", at = @At("HEAD"))
    private void felt$clearStaleFuse(CallbackInfo ci) {
        SulfurCube cube = (SulfurCube) (Object) this;
        if (cube.level() instanceof ServerLevel && cube.tickCount > 1 && this.explosionData.isEmpty() && cube.isPrimed()) {
            this.felt$resetFuse(cube);
        }
    }

    @Inject(method = "tickFuse", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;dropLeash()V"), cancellable = true)
    private void felt$primeExplosion(CallbackInfo ci, @Share("paperPrime") LocalRef<ExplosionPrimeEvent> prime) {
        SulfurCube cube = (SulfurCube) (Object) this;
        if (cube.level() instanceof ServerLevel world && world.getGameRules().get(GameRules.TNT_EXPLODES)) {
            var explosion = this.explosionData.orElseThrow();
            var event = CraftEventFactory.callExplosionPrimeEvent(cube, explosion.power(), explosion.causesFire());
            if (event.isCancelled()) {
                this.felt$resetFuse(cube);
                ci.cancel();
            } else {
                prime.set(event);
            }
        }
    }

    @Unique
    private void felt$resetFuse(SulfurCube cube) {
        cube.setFuse(PrimedTnt.NO_FUSE);
        cube.getEntityData().set(SulfurCube.MAX_FUSE, PrimedTnt.NO_FUSE);
        cube.setPermanentlyInvulnerable(false);
    }

    @WrapOperation(method = "tickFuse", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;dropLeash()V"))
    private void felt$unleashExplodingCube(SulfurCube cube, Operation<Void> drop) {
        if (cube.isLeashed()) {
            if (FeltEventFactory.shouldDropLeashOnDestruction(cube)) {
                drop.call(cube);
            } else {
                cube.removeLeash();
            }
        }
    }

    @WrapOperation(method = "tickFuse", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void felt$explodeWithEventSettings(ServerLevel world, Entity source, DamageSource damage,
                                               ExplosionDamageCalculator calculator, double x, double y, double z,
                                               float radius, boolean fire, Level.ExplosionInteraction interaction,
                                               Operation<Void> original, @Share("paperPrime") LocalRef<ExplosionPrimeEvent> prime) {
        var event = prime.get();
        original.call(world, source, damage, calculator, x, y, z, event == null ? radius : event.getRadius(),
            event == null ? fire : event.getFire(), interaction);
    }

    @Inject(method = "tickFuse", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;discard()V"))
    private void felt$removeExplodedCube(CallbackInfo ci) {
        ((SulfurCube) (Object) this).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
    }

    @Decorate(method = "mobInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void felt$shearByPlayer(SulfurCube cube, ServerLevel level, SoundSource sound, ItemStack tool,
                                    Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> drops = FeltShearing.defaultDrops(cube, level, tool);
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, cube, tool, hand, drops);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }
        var previous = FeltShearing.captureDrops(cube, tool, CraftItemStack.asNMSCopy(event.getDrops()));
        try {
            DecorationOps.callsite().invoke(cube, level, sound, tool);
        } finally {
            FeltShearing.restoreDrops(previous);
        }
    }

    @WrapOperation(method = "shear", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity felt$dropShearedEquipment(SulfurCube cube, ServerLevel level, ItemStack equipment, Vec3 offset,
                                                  Operation<ItemEntity> original, @Local(argsOnly = true) ItemStack tool) {
        List<ItemStack> drops = FeltShearing.takeDrops(cube, tool);
        if (drops == null) {
            return original.call(cube, level, equipment, offset);
        }
        ItemEntity result = null;
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                result = original.call(cube, level, drop, offset);
            }
        }
        return result;
    }

    @WrapOperation(method = "applyContactDamage", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean felt$contactDamage(Entity victim, ServerLevel level, DamageSource source, float amount,
                                       Operation<Boolean> original) {
        var bridge = (DamageSourceBridge) source;
        bridge.bridge$setCustomCausingEntity((SulfurCube) (Object) this);
        bridge.bridge$setKnownCause(EntityDamageEvent.DamageCause.CONTACT);
        return original.call(victim, level, source, amount);
    }

    @WrapWithCondition(method = "knockback", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;needsSync:Z"))
    private boolean felt$markKnockbackAfterEvent(SulfurCube cube, boolean needsSync) {
        return false;
    }

    @WrapOperation(method = "knockback", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;setDeltaMovement(DDD)V"))
    private void felt$eventKnockback(SulfurCube cube, double x, double y, double z, Operation<Void> original,
                                     @Local(argsOnly = true, ordinal = 0) double force) {
        Vec3 before = cube.getDeltaMovement();
        Vec3 knockback = ((LivingEntityBridge) cube).bridge$fireKnockbackEvent(force, new Vec3(x, y, z).subtract(before));
        if (knockback != null) {
            cube.needsSync = true;
            original.call(cube, before.x + knockback.x, before.y + knockback.y, before.z + knockback.z);
        }
    }

    @WrapOperation(method = "mobInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;equipItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean felt$swallowItem(SulfurCube cube, ItemStack held, Operation<Boolean> equip,
                                     @Local(argsOnly = true) Player player,
                                     @Share("swallowAttempted") LocalBooleanRef attempted,
                                     @Share("swallowReplacement") LocalBooleanRef replacement) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return equip.call(cube, held);
        }
        attempted.set(true);
        var original = CraftItemStack.asBukkitCopy(held);
        var event = new SulfurCubeSwallowItemEvent((org.bukkit.entity.SulfurCube) cube.bridge$getBukkitEntity(),
            (org.bukkit.entity.Player) serverPlayer.bridge$getBukkitEntity(),
            CraftItemStack.asBukkitCopy(cube.getItemBySlot(EquipmentSlot.BODY)), original);
        if (!event.callEvent()) {
            return false;
        }
        var selected = event.getNewItem();
        replacement.set(!original.equals(selected));
        return equip.call(cube, replacement.get() ? CraftItemStack.asNMSCopy(selected) : held);
    }

    @WrapWithCondition(method = "mobInteract", at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    private boolean felt$consumeOnlyOriginal(ItemStack held, int amount, LivingEntity consumer,
                                             @Share("swallowReplacement") LocalBooleanRef replacement) {
        return !replacement.get();
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void felt$syncSwallow(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir,
                                  @Share("swallowAttempted") LocalBooleanRef attempted) {
        if (attempted.get() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.sendAllDataToRemote();
        }
    }

    @Inject(method = "playerPush", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/entity/ai/attributes/Attributes;KNOCKBACK_RESISTANCE:Lnet/minecraft/core/Holder;"),
        cancellable = true)
    private void felt$collideWithPusher(Player player, CallbackInfo ci, @Local(ordinal = 0) Entity pusher) {
        SulfurCube cube = (SulfurCube) (Object) this;
        if (cube.level() instanceof ServerLevel && EntityCollideWithEntityEvent.getHandlerList().hasListeners()
            && !new EntityCollideWithEntityEvent(cube.bridge$getBukkitEntity(), pusher.bridge$getBukkitEntity()).callEvent()) {
            ci.cancel();
        }
    }
}
