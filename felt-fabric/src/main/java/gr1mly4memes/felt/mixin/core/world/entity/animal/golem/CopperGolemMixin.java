package gr1mly4memes.felt.mixin.core.world.entity.animal.golem;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(CopperGolem.class)
public abstract class CopperGolemMixin implements gr1mly4memes.felt.bridge.core.world.entity.ShearableBridge {

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean felt$statueChangeBlock(ServerLevel level, BlockPos pos, BlockState state) throws Throwable {
        if (!CraftEventFactory.callEntityChangeBlockEvent((CopperGolem) (Object) this, pos, state)) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, state);
    }

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;dropPreservedEquipment(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void felt$statueForceDrops(CopperGolem golem, ServerLevel level) throws Throwable {
        var bridge = (EntityBridge) golem;
        boolean previous = bridge.bridge$isForceDrops();
        bridge.bridge$setForceDrops(true);
        try {
            DecorationOps.callsite().invoke(golem, level);
        } finally {
            bridge.bridge$setForceDrops(previous);
        }
    }

    @Decorate(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;discard()V"))
    private void felt$statueRemoveCause(CopperGolem golem) throws Throwable {
        ((EntityBridge) golem).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
        DecorationOps.callsite().invoke(golem);
    }

    @Unique private List<ItemStack> felt$shearDrops;

    @Override
    public List<ItemStack> bridge$swapShearDrops(List<ItemStack> drops) {
        List<ItemStack> previous = this.felt$shearDrops;
        this.felt$shearDrops = drops;
        return previous;
    }

    @Decorate(method = "mobInteract", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void felt$onShear(CopperGolem golem, ServerLevel level, SoundSource source, ItemStack tool, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaultDrops = new ArrayList<>(1);
        defaultDrops.add(golem.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).copy());
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, golem, tool, hand, defaultDrops);
        if (event != null && event.isCancelled()) {
            ((PlayerBridge) player).bridge$cancelCopperShear(golem);
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }

        List<ItemStack> drops = event == null ? defaultDrops : CraftItemStack.asNMSCopy(event.getDrops());
        List<ItemStack> previousDrops = this.felt$shearDrops;
        this.felt$shearDrops = drops;
        try {
            DecorationOps.callsite().invoke(golem, level, source, tool);
        } finally {
            this.felt$shearDrops = previousDrops;
        }
    }

    @WrapOperation(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"), require = 1, allow = 1)
    private ItemEntity felt$dropShearedItems(CopperGolem golem, ServerLevel level, ItemStack stack, float height, Operation<ItemEntity> original) {
        List<ItemStack> drops = this.felt$shearDrops;
        if (drops == null) {
            return original.call(golem, level, stack, height);
        }
        this.felt$shearDrops = List.of();
        var bridge = (EntityBridge) golem;
        boolean previousForceDrops = bridge.bridge$isForceDrops();
        bridge.bridge$setForceDrops(true);
        try {
            ItemEntity result = null;
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    result = original.call(golem, level, drop, height);
                }
            }
            return result;
        } finally {
            bridge.bridge$setForceDrops(previousForceDrops);
        }
    }
}
