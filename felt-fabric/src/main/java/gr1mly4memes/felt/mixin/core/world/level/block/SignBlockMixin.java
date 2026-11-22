package gr1mly4memes.felt.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.bridge.core.world.level.block.SignBlockEditBridge;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.sign.Side;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.player.PlayerSignOpenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignBlock.class)
public abstract class SignBlockMixin implements SignBlockEditBridge {

    // @formatter:off
    @Shadow public abstract void openTextEdit(Player player, SignBlockEntity sign, SignTextSlot slot);
    // @formatter:on

    private transient PlayerSignOpenEvent.Cause felt$edit;
    private transient Player felt$editPlayer;
    private transient SignBlockEntity felt$editSign;
    private transient SignTextSlot felt$editSlot;

    @WrapOperation(method = "setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SignBlock;openTextEdit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;)V"))
    private void felt$deferSignEditor(SignBlock block, Player player, SignBlockEntity sign, SignTextSlot slot,
                                         Operation<Void> original, Level level, BlockPos pos, BlockState state,
                                         LivingEntity placer, ItemStack stack) {
        // Native server/player/sign/wax/editable-text gates have already run at this callsite.
        FeltEventFactory.deferSignEditor(block, player, sign, slot, original, pos, level, stack);
    }

    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SignBlock;openTextEdit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;)V"))
    private void felt$interactEdit(SignBlock block, Player player, SignBlockEntity sign, SignTextSlot slot, Operation<Void> original) {
        var previous = felt$edit;
        var previousPlayer = felt$editPlayer;
        var previousSign = felt$editSign;
        var previousSlot = felt$editSlot;
        felt$edit = PlayerSignOpenEvent.Cause.INTERACT;
        felt$editPlayer = player;
        felt$editSign = sign;
        felt$editSlot = slot;
        try {
            original.call(block, player, sign, slot);
        } finally {
            felt$edit = previous;
            felt$editPlayer = previousPlayer;
            felt$editSign = previousSign;
            felt$editSlot = previousSlot;
        }
    }

    public void openTextEdit(Player player, SignBlockEntity sign, boolean isFrontText, PlayerSignOpenEvent.Cause cause) {
        this.openTextEdit(player, sign, isFrontText ? SignTextSlot.FRONT : SignTextSlot.BACK, cause);
    }

    public void openTextEdit(Player player, SignBlockEntity sign, SignTextSlot slot, PlayerSignOpenEvent.Cause cause) {
        var previous = felt$edit;
        var previousPlayer = felt$editPlayer;
        var previousSign = felt$editSign;
        var previousSlot = felt$editSlot;
        felt$edit = cause;
        felt$editPlayer = player;
        felt$editSign = sign;
        felt$editSlot = slot;
        try {
            this.openTextEdit(player, sign, slot);
        } finally {
            felt$edit = previous;
            felt$editPlayer = previousPlayer;
            felt$editSign = previousSign;
            felt$editSlot = previousSlot;
        }
    }

    @Override
    public void bridge$openTextEdit(Player player, SignBlockEntity sign, SignTextSlot slot,
                                   PlayerSignOpenEvent.Cause cause, Operation<Void> original) {
        var previous = felt$edit;
        var previousPlayer = felt$editPlayer;
        var previousSign = felt$editSign;
        var previousSlot = felt$editSlot;
        felt$edit = cause;
        felt$editPlayer = player;
        felt$editSign = sign;
        felt$editSlot = slot;
        try {
            original.call((SignBlock) (Object) this, player, sign, slot);
        } finally {
            felt$edit = previous;
            felt$editPlayer = previousPlayer;
            felt$editSign = previousSign;
            felt$editSlot = previousSlot;
        }
    }

    @Decorate(method = "openTextEdit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;)V", inject = true, at = @At("HEAD"))
    private void felt$signEdit(Player player, SignBlockEntity signBlockEntity, SignTextSlot slot) throws Throwable {
        boolean owned = felt$edit != null && (felt$editSign == null
            || (felt$editPlayer == player && felt$editSign == signBlockEntity && felt$editSlot == slot));
        var cause = owned ? felt$edit : PlayerSignOpenEvent.Cause.UNKNOWN;
        var pendingCause = felt$edit;
        var pendingPlayer = felt$editPlayer;
        var pendingSign = felt$editSign;
        var pendingSlot = felt$editSlot;
        // Mask even a mismatching owner's event callbacks: listeners cannot borrow it.
        // A matching native entry consumes it; a mismatch restores it after dispatch.
        // Identity-identical pre-continuation opens inside an opaque third-party Operation
        // cannot be distinguished by this native ABI (see the revision compatibility report).
        felt$edit = null;
        felt$editPlayer = null;
        felt$editSign = null;
        boolean allowed;
        try {
            var paperCause = switch (cause) {
                case PLACE -> PlayerOpenSignEvent.Cause.PLACE;
                case INTERACT -> PlayerOpenSignEvent.Cause.INTERACT;
                case PLUGIN -> PlayerOpenSignEvent.Cause.PLUGIN;
                case UNKNOWN -> PlayerOpenSignEvent.Cause.UNKNOWN;
            };
            var state = CraftBlockStates.getBlockState(CraftBlock.at(signBlockEntity.getLevel(), signBlockEntity.getBlockPos()));
            org.bukkit.block.Sign sign = state instanceof org.bukkit.block.Sign placedSign ? placedSign
                : new CraftSign<>(((WorldBridge) signBlockEntity.getLevel()).bridge$getWorld(), signBlockEntity, true);
            var event = new PlayerOpenSignEvent(
                (org.bukkit.entity.Player) ((EntityBridge) player).bridge$getBukkitEntity(), sign,
                slot == SignTextSlot.FRONT ? Side.FRONT : Side.BACK, paperCause);
            allowed = event.callEvent()
                && (PlayerSignOpenEvent.getHandlerList().getRegisteredListeners().length == 0
                || CraftEventFactory.callPlayerSignOpenEvent(player, signBlockEntity, slot, cause));
        } finally {
            if (!owned) {
                felt$edit = pendingCause;
                felt$editPlayer = pendingPlayer;
                felt$editSign = pendingSign;
                felt$editSlot = pendingSlot;
            }
        }
        if (!allowed) {
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
