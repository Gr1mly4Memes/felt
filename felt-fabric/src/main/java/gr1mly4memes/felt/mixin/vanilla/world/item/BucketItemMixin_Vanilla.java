package gr1mly4memes.felt.mixin.vanilla.world.item;

import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.world.level.LevelAccessorBridge;
import gr1mly4memes.felt.bridge.core.world.item.BucketItemBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin_Vanilla implements BucketItemBridge {
    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BucketItem;emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)Z"))
    private void felt$capture(Level worldIn, Player playerIn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Local BlockHitResult result, @Local ItemStack stack) {
        felt$setDirection(result.getDirection());
        felt$setClick(result.getBlockPos());
        felt$setHand(hand);
        felt$setStack(stack);
    }

    @Inject(method = "emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)Z", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"), require = 1, allow = 1)
    private void felt$bucketEmpty(LivingEntity player, Level worldIn, BlockPos posIn, BlockHitResult rayTrace, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 2) boolean placeLiquid) {
        if (placeLiquid && LevelAccessorBridge.from(worldIn) instanceof LevelAccessorBridge bridge && player != null && felt$getStack() != null) {
            if (!(player instanceof ServerPlayer bucketPlayer)) {
                return;
            }
            PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent(bridge.bridge$getMinecraftWorld(), bucketPlayer, posIn, felt$getClick(), felt$getDirection(), felt$getStack(), felt$getHand() == null ? InteractionHand.MAIN_HAND : felt$getHand());
            if (event.isCancelled()) {
                ((ServerPlayer) bucketPlayer).connection.send(new ClientboundBlockUpdatePacket(worldIn, posIn));
                ((ServerPlayerBridge) bucketPlayer).bridge$getBukkitEntity().updateInventory();
                cir.setReturnValue(false);
            } else {
                // Preserve the original/modded remainder unless a plugin actually changed it.
                org.bukkit.inventory.ItemStack remainder = event.getItemStack();
                if (remainder != null && !remainder.equals(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BUCKET))) {
                    felt$setCaptureItem(remainder.clone());
                }
            }
        }
    }
}
