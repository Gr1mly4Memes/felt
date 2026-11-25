package gr1mly4memes.felt.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.world.LootableDataBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.mod.server.world.FeltLootReplenishment;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxLootMixin {
    @WrapOperation(method = "playerWillDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;unpackLootTable(Lnet/minecraft/world/entity/player/Player;)V"))
    private void felt$clearLootBeforePlayerDrop(ShulkerBoxBlockEntity box, Player player, Operation<Void> original) {
        FeltLootReplenishment.unpackAndClear(box, player);
    }
    @WrapMethod(method = "getDrops")
    private List<ItemStack> felt$clearRefillDataFromItem(BlockState state, LootParams.Builder params, Operation<List<ItemStack>> original) {
        var entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(entity instanceof ShulkerBoxBlockEntity box)
            || (((LootableDataBridge)box).bridge$lootableData().getLastFill() == -1
                && ((WorldBridge)params.getLevel()).bridge$paperConfig().lootables.retainUnlootedShulkerBoxLootTableOnNonPlayerBreak)) return original.call(state, params);
        var table = box.getLootTable();
        box.setLootTable(null);
        try { return original.call(state, params); }
        finally { box.setLootTable(table); }
    }
}
