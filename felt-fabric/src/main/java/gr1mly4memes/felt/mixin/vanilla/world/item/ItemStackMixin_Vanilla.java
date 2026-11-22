package gr1mly4memes.felt.mixin.vanilla.world.item;

import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin_Vanilla {

    // @formatter:off
    @Shadow private int count;
    @Shadow public abstract boolean canPlaceOnBlockInAdventureMode(BlockInWorld blockInWorld);
    @Shadow public abstract Item getItem();
    @Shadow public abstract ItemStack copy();
    // @formatter:on

    @WrapMethod(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;")
    private InteractionResult felt$signPlacement(UseOnContext context, Operation<InteractionResult> original) {
        return gr1mly4memes.felt.mod.util.TreeCapture.useOn(
            (ItemStack) (Object) this, context, () -> FeltEventFactory.useSignItem(context, original));
    }

    @Overwrite
    public InteractionResult useOn(UseOnContext useOnContext) {
        Player player = useOnContext.getPlayer();
        BlockPos blockPos = useOnContext.getClickedPos();
        if (player != null && !player.getAbilities().mayBuild && !this.canPlaceOnBlockInAdventureMode(new BlockInWorld(useOnContext.getLevel(), blockPos, false))) {
            return InteractionResult.PASS;
        } else {
            Item item = this.getItem();
            InteractionResult interactionResult;
            ItemStack oldStack = this.copy();
            if (!(item instanceof BucketItem || item instanceof SolidBucketItem)) {
                ((WorldBridge) useOnContext.getLevel()).bridge$platform$startCaptureBlockBreak();
            }
            interactionResult = item.useOn(useOnContext);
            ((WorldBridge) useOnContext.getLevel()).bridge$platform$endCaptureBlockBreak();
            if (player != null && interactionResult instanceof InteractionResult.Success success && success.wasItemInteraction()) {
                interactionResult = FeltEventFactory.onBlockPlace(useOnContext, player, oldStack, (ItemStack) (Object) this, interactionResult);
                if (interactionResult != InteractionResult.FAIL) {
                    gr1mly4memes.felt.mod.util.TreeCapture.award(
                        player, () -> player.awardStat(Stats.ITEM_USED.get(item)));
                }
            }

            ((WorldBridge) useOnContext.getLevel()).bridge$getCapturedBlockEntity().clear();
            ((WorldBridge) useOnContext.getLevel()).bridge$getCapturedBlockState().clear();
            return interactionResult;
        }
    }

    @Decorate(method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;hasInfiniteMaterials()Z"))
    private boolean felt$forceItemDamage(ServerPlayer owner) throws Throwable {
        boolean infiniteMaterials = (boolean) DecorationOps.callsite().invoke(owner);
        return infiniteMaterials && !FeltCaptures.isItemDamageForced((ItemStack) (Object) this, owner);
    }

    @WrapOperation(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;)I"))
    private int felt$itemDamage(ItemStack stack, int amount, ServerLevel level, ServerPlayer damager, Operation<Integer> original) {
        // Preserve the existing event gate, but do not eventize hurtWithoutBreaking.
        LivingEntity apiOwner = FeltCaptures.getItemDamageOwner(stack, damager);
        boolean eventEligible = amount > 0 && stack.isDamageableItem()
            && damager != null && !damager.hasInfiniteMaterials();
        int result = original.call(stack, amount, level, damager);
        if (apiOwner != null && !(apiOwner instanceof ServerPlayer)) {
            if (result > 0) {
                EntityDamageItemEvent event = new EntityDamageItemEvent(((LivingEntityBridge) apiOwner).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(stack), result);
                return event.callEvent() ? event.getDamage() : 0;
            }
            return result;
        }
        if (damager != null && (apiOwner != null ? result > 0 : eventEligible)) {
            PlayerItemDamageEvent event = new PlayerItemDamageEvent(((ServerPlayerBridge) damager).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(stack), result, amount);
            event.getPlayer().getServer().getPluginManager().callEvent(event);
            if (result != event.getDamage() || event.isCancelled()) {
                event.getPlayer().updateInventory();
            }
            if (event.isCancelled()) {
                return 0;
            }
            result = event.getDamage();
        }
        return result;
    }

    @Inject(method = "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void felt$itemBreak(int amount, @Nullable ServerPlayer serverPlayer, Consumer<Item> onBroken, CallbackInfo ci) {
        if (this.count == 1 && serverPlayer != null) {
            CraftEventFactory.callPlayerItemBreakEvent(serverPlayer, (ItemStack) (Object) this);
        }
    }
}
