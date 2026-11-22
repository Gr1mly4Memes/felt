package gr1mly4memes.felt.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.mod.util.BucketUseContext;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.world.level.LevelAccessorBridge;
import gr1mly4memes.felt.bridge.core.world.item.BucketItemBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.DummyLevelAccessor;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin implements BucketItemBridge {

    // @formatter:off
    @Shadow public abstract boolean emptyContents(@Nullable LivingEntity player, Level worldIn, BlockPos posIn, @javax.annotation.Nullable BlockHitResult rayTrace);
    // @formatter:on

    // Use Decorate to capture locals.
    @Decorate(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack felt$bucketFill(BucketPickup pickup, LivingEntity entity, LevelAccessor worldIn, BlockPos pos, BlockState state,
                                          @Local(ordinal = 0) InteractionHand handIn, @Local(ordinal = 0) ItemStack stack, @Local(ordinal = 0) BlockHitResult result) throws Throwable {
        if (entity instanceof Player playerIn && LevelAccessorBridge.from(worldIn) instanceof LevelAccessorBridge bridge) {
            ItemStack dummyFluid = pickup.pickupBlock(playerIn, DummyLevelAccessor.INSTANCE, pos, state);
            if (dummyFluid.isEmpty()) {
                return (ItemStack) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            }
            PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent(bridge.bridge$getMinecraftWorld(), playerIn, pos, pos, result.getDirection(), stack, dummyFluid.getItem(), handIn);
            if (event.isCancelled()) {
                ((ServerPlayer) playerIn).connection.send(new ClientboundBlockUpdatePacket(worldIn, pos));
                ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity().updateInventory();
                return (ItemStack) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            } else {
                felt$setCaptureItem(event.getItemStack());
            }
        }
        return (ItemStack) DecorationOps.callsite().invoke(pickup, entity, worldIn, pos, state);
    }

    @WrapMethod(method = "use")
    private InteractionResult felt$useContext(Level level, Player player, InteractionHand hand, Operation<InteractionResult> original) {
        BucketUseContext previous = felt$context;
        felt$context = new BucketUseContext();
        try {
            return original.call(level, player, hand);
        } finally {
            felt$context = previous;
        }
    }

    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack felt$useEventItem(ItemStack itemStack) {
        return felt$getCaptureItem() == null ? itemStack : CraftItemStack.asNMSCopy(felt$getCaptureItem());
    }

    public boolean emptyContents(Player entity, Level world, BlockPos pos, @Nullable BlockHitResult result, Direction direction, BlockPos clicked, ItemStack itemstack, InteractionHand hand) {
        BucketUseContext previous = felt$context;
        felt$context = new BucketUseContext();
        felt$setDirection(direction);
        felt$setClick(clicked);
        felt$setHand(hand);
        felt$setStack(itemstack);
        try {
            return this.emptyContents(entity, world, pos, result);
        } finally {
            felt$context = previous;
        }
    }

    @Unique
    private BucketUseContext felt$context;

    @Nullable
    @Override
    public Direction felt$getDirection() {
        BucketUseContext context = this.felt$context;
        return context == null ? null : context.direction;
    }

    @Override
    public void felt$setDirection(@Nullable Direction value) {
        BucketUseContext context = this.felt$context;
        if (context != null) context.direction = value;
    }

    @Nullable
    @Override
    public BlockPos felt$getClick() {
        BucketUseContext context = this.felt$context;
        return context == null ? null : context.click;
    }

    @Override
    public void felt$setClick(@Nullable BlockPos value) {
        BucketUseContext context = this.felt$context;
        if (context != null) context.click = value;
    }

    @Nullable
    @Override
    public InteractionHand felt$getHand() {
        BucketUseContext context = this.felt$context;
        return context == null ? null : context.hand;
    }

    @Override
    public void felt$setHand(@Nullable InteractionHand value) {
        BucketUseContext context = this.felt$context;
        if (context != null) context.hand = value;
    }

    @Nullable
    @Override
    public ItemStack felt$getStack() {
        BucketUseContext context = this.felt$context;
        return context == null ? null : context.stack;
    }

    @Override
    public void felt$setStack(@Nullable ItemStack value) {
        BucketUseContext context = this.felt$context;
        if (context != null) context.stack = value;
    }

    @Nullable
    @Override
    public org.bukkit.inventory.ItemStack felt$getCaptureItem() {
        BucketUseContext context = this.felt$context;
        return context == null ? null : context.captureItem;
    }

    @Override
    public void felt$setCaptureItem(@Nullable org.bukkit.inventory.ItemStack value) {
        BucketUseContext context = this.felt$context;
        if (context != null) context.captureItem = value;
    }
}
