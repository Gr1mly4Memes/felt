package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.FlintAndSteelDispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.TNTPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelDispenseItemBehavior.class)
public abstract class FlintAndSteelDispenseBehaviorMixin extends OptionalDispenseItemBehavior {
    @Unique private static final ThreadLocal<BooleanSupplier> felt$cubeIgnitionCheck = new ThreadLocal<>();

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/dispenser/FlintAndSteelDispenseItemBehavior;tryIgniteExplosiveEntities(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean felt$checkCubeIgnition(ServerLevel level, BlockPos target, Operation<Boolean> ignite,
                                          @Local(argsOnly = true) BlockSource source, @Local(argsOnly = true) ItemStack stack,
                                          @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        BooleanSupplier previous = felt$cubeIgnitionCheck.get();
        felt$cubeIgnitionCheck.set(() -> this.felt$allowDispense(source, target, stack, cir));
        try {
            return ignite.call(level, target);
        } finally {
            if (previous == null) {
                felt$cubeIgnitionCheck.remove();
            } else {
                felt$cubeIgnitionCheck.set(previous);
            }
        }
    }

    @WrapOperation(method = "tryIgniteExplosiveEntities", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;primeTime(Z)Z"))
    private static boolean felt$igniteCube(SulfurCube cube, boolean imminent, Operation<Boolean> ignite) {
        var check = felt$cubeIgnitionCheck.get();
        return (check == null || check.getAsBoolean()) && ignite.call(cube, imminent);
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean felt$igniteBlock(ServerLevel level, BlockPos target, BlockState replacement, Operation<Boolean> place,
                                     @Local(argsOnly = true) BlockSource source, @Local(argsOnly = true) ItemStack stack,
                                     @Share("skipFirePlacementEvent") LocalBooleanRef skipPlacement,
                                     @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        if (!this.felt$allowDispense(source, target, stack, cir)) {
            return false;
        }
        if (replacement.getBlock() instanceof BaseFireBlock
            && CraftEventFactory.callBlockIgniteEvent(level, target, source.pos()).isCancelled()) {
            skipPlacement.set(true);
            return false;
        }
        return place.call(level, target, replacement);
    }

    @WrapWithCondition(method = "execute", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/server/level/ServerLevel;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V"))
    private boolean felt$emitFirePlacement(ServerLevel level, Entity entity, Holder<GameEvent> event, BlockPos position,
                                           @Share("skipFirePlacementEvent") LocalBooleanRef skipPlacement) {
        return !skipPlacement.get();
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean felt$primeTnt(Level level, BlockPos target, LivingEntity actor, ItemStack stack, Operation<Boolean> prime,
                                  @Local(argsOnly = true) BlockSource source, @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return prime.call(level, target, actor, stack);
        }
        return this.felt$allowDispense(source, target, stack, cir)
            && CraftEventFactory.callTNTPrimeEvent(level, target, TNTPrimeEvent.PrimeCause.DISPENSER, null, source.pos())
            && prime.call(level, target, actor, stack);
    }

    @Unique
    private boolean felt$allowDispense(BlockSource source, BlockPos target, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack veto = CraftEventFactory.handleBlockDispenseEvent(source, target, stack, this);
        if (veto == null) {
            return true;
        }
        this.setSuccess(false);
        cir.setReturnValue(veto);
        return false;
    }
}
