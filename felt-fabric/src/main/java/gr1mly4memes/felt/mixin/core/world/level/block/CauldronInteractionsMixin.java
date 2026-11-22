package gr1mly4memes.felt.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.mod.server.block.CauldronHooks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * Bridges bucket, bottle and washing interactions after their native
 * eligibility points and before their first player-item mutation.
 */
@Mixin(CauldronInteractions.class)
public abstract class CauldronInteractionsMixin {

    @Unique
    private static final String FELT$FILL_BUCKET = "fillBucket(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Predicate;Lnet/minecraft/sounds/SoundEvent;)Lnet/minecraft/world/InteractionResult;";
    @Unique
    private static final String FELT$EMPTY_BUCKET = "emptyBucket(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/sounds/SoundEvent;)Lnet/minecraft/world/InteractionResult;";
    @Unique
    private static final String FELT$CREATE_FILLED_RESULT = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;";
    @Unique
    private static final String FELT$SET_BLOCK_AND_UPDATE = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z";

    /** canFill and the server-side branch have already passed at this point. */
    @Inject(method = FELT$FILL_BUCKET, at = @At(value = "INVOKE", target = FELT$CREATE_FILLED_RESULT), cancellable = true, require = 1, expect = 1, allow = 1)
    private static void felt$beforeBucketFill(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, ItemStack itemInHand, ItemStack newItem, Predicate<BlockState> canFill,
            SoundEvent soundEvent, CallbackInfoReturnable<InteractionResult> cir,
            @Share(value = "bucketFillEventState", namespace = "felt:cauldron") LocalRef<BlockState> eventState) {
            BlockState selected = felt$prepareChange(level, pos, player, Blocks.CAULDRON.defaultBlockState(),
            CauldronLevelChangeEvent.ChangeReason.BUCKET_FILL);
        if (selected == null) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else {
            eventState.set(selected);
        }
    }

    @WrapOperation(method = FELT$FILL_BUCKET, at = @At(value = "INVOKE", target = FELT$SET_BLOCK_AND_UPDATE), require = 1, expect = 1, allow = 1)
    private static boolean felt$keepBucketFillEventState(Level level, BlockPos pos, BlockState nativeState,
            Operation<Boolean> original,
            @Share(value = "bucketFillEventState", namespace = "felt:cauldron") LocalRef<BlockState> eventState) {
        BlockState selected = eventState.get();
        // Preserve Operation composition while passing the event-selected state to
        // the existing setter chain exactly once.
        return original.call(level, pos, selected == null ? nativeState : selected);
    }

    /** Reached only when the caller's native underwater guards have passed. */
    @Inject(method = FELT$EMPTY_BUCKET, at = @At(value = "INVOKE", target = FELT$CREATE_FILLED_RESULT), cancellable = true, require = 1, expect = 1, allow = 1)
    private static void felt$beforeBucketEmpty(Level level, BlockPos pos, Player player, InteractionHand hand,
            ItemStack itemInHand, BlockState newState, SoundEvent soundEvent,
            CallbackInfoReturnable<InteractionResult> cir,
            @Share(value = "bucketEmptyEventState", namespace = "felt:cauldron") LocalRef<BlockState> eventState) {
        BlockState selected = felt$prepareChange(level, pos, player, newState,
                CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY);
        if (selected == null) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else {
            eventState.set(selected);
        }
    }

    @WrapOperation(method = FELT$EMPTY_BUCKET, at = @At(value = "INVOKE", target = FELT$SET_BLOCK_AND_UPDATE), require = 1, expect = 1, allow = 1)
    private static boolean felt$keepBucketEmptyEventState(Level level, BlockPos pos, BlockState nativeState,
            Operation<Boolean> original,
            @Share(value = "bucketEmptyEventState", namespace = "felt:cauldron") LocalRef<BlockState> eventState) {
        BlockState selected = eventState.get();
        return original.call(level, pos, selected == null ? nativeState : selected);
    }

    @Inject(method = "lambda$bootStrap$0", at = @At(value = "INVOKE", target = FELT$CREATE_FILLED_RESULT),
        cancellable = true, require = 1, expect = 1, allow = 1)
    private static void felt$emptyBottle(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir,
            @Share("felt$bottleWashState") LocalRef<BlockState> selected) {
        BlockState proposed = felt$prepareChange(level, pos, player, Blocks.WATER_CAULDRON.defaultBlockState(),
            CauldronLevelChangeEvent.ChangeReason.BOTTLE_EMPTY);
        if (proposed == null) cir.setReturnValue(InteractionResult.SUCCESS);
        else selected.set(proposed);
    }

    @Inject(method = "lambda$bootStrap$4", at = @At(value = "INVOKE", target = FELT$CREATE_FILLED_RESULT),
        cancellable = true, require = 1, expect = 1, allow = 1)
    private static void felt$addBottle(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir,
            @Share("felt$bottleWashState") LocalRef<BlockState> selected) {
        BlockState proposed = felt$prepareChange(level, pos, player, state.cycle(LayeredCauldronBlock.LEVEL),
            CauldronLevelChangeEvent.ChangeReason.BOTTLE_EMPTY);
        if (proposed == null) cir.setReturnValue(InteractionResult.SUCCESS);
        else selected.set(proposed);
    }

    @Inject(method = "lambda$bootStrap$3", at = @At(value = "INVOKE", target = FELT$CREATE_FILLED_RESULT),
        cancellable = true, require = 1, expect = 1, allow = 1)
    private static void felt$fillBottle(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir,
            @Share("felt$bottleWashState") LocalRef<BlockState> selected) {
        BlockState proposed = felt$prepareChange(level, pos, player, felt$lowerState(state),
            CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL);
        if (proposed == null) cir.setReturnValue(InteractionResult.SUCCESS);
        else selected.set(proposed);
    }

    @Inject(method = "shulkerBoxInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;transmuteCopy(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/item/ItemStack;"),
        cancellable = true, require = 1, expect = 1, allow = 1)
    private static void felt$washShulker(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir,
            @Share("felt$bottleWashState") LocalRef<BlockState> selected) {
        BlockState proposed = felt$prepareChange(level, pos, player, felt$lowerState(state),
            CauldronLevelChangeEvent.ChangeReason.SHULKER_WASH);
        if (proposed == null) cir.setReturnValue(InteractionResult.SUCCESS);
        else selected.set(proposed);
    }

    @Inject(method = "bannerInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"),
        cancellable = true, require = 1, expect = 1, allow = 1)
    private static void felt$washBanner(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir,
            @Share("felt$bottleWashState") LocalRef<BlockState> selected) {
        BlockState proposed = felt$prepareChange(level, pos, player, felt$lowerState(state),
            CauldronLevelChangeEvent.ChangeReason.BANNER_WASH);
        if (proposed == null) cir.setReturnValue(InteractionResult.SUCCESS);
        else selected.set(proposed);
    }

    @Inject(method = "dyedItemIteration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"),
        cancellable = true, require = 1, expect = 1, allow = 1)
    private static void felt$washArmor(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir,
            @Share("felt$bottleWashState") LocalRef<BlockState> selected) {
        BlockState proposed = felt$prepareChange(level, pos, player, felt$lowerState(state),
            CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH);
        if (proposed == null) cir.setReturnValue(InteractionResult.SUCCESS);
        else selected.set(proposed);
    }

    @Unique
    private static BlockState felt$lowerState(BlockState state) {
        int remaining = state.getValue(LayeredCauldronBlock.LEVEL) - 1;
        return remaining == 0 ? Blocks.CAULDRON.defaultBlockState() : state.setValue(LayeredCauldronBlock.LEVEL, remaining);
    }

    @WrapOperation(method = {"lambda$bootStrap$0", "lambda$bootStrap$4"},
        at = @At(value = "INVOKE", target = FELT$SET_BLOCK_AND_UPDATE), require = 2, expect = 2, allow = 2)
    private static boolean felt$bottleCommit(Level level, BlockPos pos, BlockState state, Operation<Boolean> original,
            @Share("felt$bottleWashState") LocalRef<BlockState> selected) {
        return original.call(level, pos, selected.get() == null ? state : selected.get());
    }

    @WrapOperation(method = {"lambda$bootStrap$3", "shulkerBoxInteraction", "bannerInteraction", "dyedItemIteration"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LayeredCauldronBlock;lowerFillLevel(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"),
        require = 4, expect = 4, allow = 4)
    private static void felt$washCommit(BlockState state, Level level, BlockPos pos, Operation<Void> original,
            @Share("felt$bottleWashState") LocalRef<BlockState> selected) {
        CauldronHooks.withPreparedInteraction(level, pos, selected.get(), () -> original.call(state, level, pos));
    }

    @Unique
    private static BlockState felt$prepareChange(Level level, BlockPos pos, Player player,
            BlockState requested, CauldronLevelChangeEvent.ChangeReason reason) {
        CraftBlockState proposed = CraftBlockStates.getBlockState(level, pos);
        proposed.setBlock(requested);
        CauldronLevelChangeEvent event = new CauldronLevelChangeEvent(CraftBlock.at(level, pos),
                ((EntityBridge) player).bridge$getBukkitEntity(), reason, proposed);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }
        return proposed.getHandle();
    }
}
