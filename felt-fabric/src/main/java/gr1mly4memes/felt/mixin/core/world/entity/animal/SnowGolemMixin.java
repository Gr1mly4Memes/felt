package gr1mly4memes.felt.mixin.core.world.entity.animal;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.world.damagesource.DamageSourcesBridge;
import gr1mly4memes.felt.bridge.core.world.level.LevelAccessorBridge;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import gr1mly4memes.felt.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin extends PathfinderMobMixin {

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;onFire()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource felt$useMelting(DamageSources instance) {
        return ((DamageSourcesBridge) instance).bridge$melting();
    }

    @Decorate(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean felt$blockForm(Level world, BlockPos pos, BlockState newState) throws Throwable {
        if (LevelAccessorBridge.from(world) instanceof LevelAccessorBridge bridge) {
            final var event = FeltEventFactory.callBlockFormEvent(bridge.bridge$getMinecraftWorld(), pos, newState, 3, (SnowGolem) (Object) this);
            if (event != null) {
                if (event.isCancelled()) {
                    return false;
                }
                newState = ((CraftBlockState) event.getNewState()).getHandle();
            }
        }
        return (boolean) DecorationOps.callsite().invoke(world, pos, newState);
    }

    @WrapOperation(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/SnowGolem;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemInstance;Ljava/util/function/BiConsumer;)V"))
    private void felt$forceDrop(SnowGolem snowGolem, ServerLevel level, ResourceKey<LootTable> lootTable, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> dropper, Operation<Void> original) throws Throwable {
        boolean previous = this.forceDrops;
        this.forceDrops = true;
        try {
            var selection = FeltCaptures.snowGolemShearDrops;
            List<ItemStack> drops = null;
            if (selection != null && selection.getLeft() == snowGolem && selection.getMiddle() == tool
                && net.minecraft.world.level.storage.loot.BuiltInLootTables.SHEAR_SNOW_GOLEM.equals(lootTable)) {
                drops = selection.getRight();
                FeltCaptures.snowGolemShearDrops = null;
            }
            if (drops == null) {
                original.call(snowGolem, level, lootTable, tool, dropper);
            } else {
                for (ItemStack drop : drops) {
                    dropper.accept(level, drop);
                }
            }
        } finally {
            this.forceDrops = previous;
        }
    }
}
