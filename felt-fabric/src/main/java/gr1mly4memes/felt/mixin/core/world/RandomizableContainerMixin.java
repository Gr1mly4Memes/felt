package gr1mly4memes.felt.mixin.core.world;

import com.destroystokyo.paper.loottable.PaperLootableInventoryData;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.world.LootableDataBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RandomizableContainer.class)
public interface RandomizableContainerMixin extends LootableDataBridge {
    @Override default PaperLootableInventoryData bridge$lootableData() { return null; }
    @WrapMethod(method = "tryLoadLootTable")
    private boolean felt$readLootData(ValueInput input, Operation<Boolean> original) {
        boolean hasTable = original.call(input);
        PaperLootableInventoryData data = this.bridge$lootableData();
        if (data == null) return hasTable;
        if (((RandomizableContainer)this).getLootTable() != null) data.loadNbt(input);
        return false; // Retained loot tables must not suppress the container's item data.
    }
    @WrapMethod(method = "trySaveLootTable")
    private boolean felt$writeLootData(ValueOutput output, Operation<Boolean> original) {
        boolean hasTable = original.call(output);
        PaperLootableInventoryData data = this.bridge$lootableData();
        if (data == null) return hasTable;
        if (((RandomizableContainer)this).getLootTable() != null) data.saveNbt(output);
        return false;
    }
    @WrapMethod(method = "unpackLootTable")
    private void felt$replenishOnlyWhenDue(Player player, Operation<Void> original) {
        PaperLootableInventoryData data = this.bridge$lootableData();
        var container = (RandomizableContainer)this;
        if (data == null || container.getLevel() == null || container.getLevel().getServer() == null
            || data.shouldReplenish(container, PaperLootableInventoryData.CONTAINER, player)) original.call(player);
    }
    @WrapOperation(method = "unpackLootTable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/RandomizableContainer;setLootTable(Lnet/minecraft/resources/ResourceKey;)V"))
    private void felt$retainRefillTable(RandomizableContainer container, ResourceKey<LootTable> table, Operation<Void> original, Player player) {
        PaperLootableInventoryData data = this.bridge$lootableData();
        if (gr1mly4memes.felt.mod.server.world.FeltLootReplenishment.forceClear(container) || data == null
            || data.shouldClearLootTable(container, PaperLootableInventoryData.CONTAINER, player)) original.call(container, table);
    }
}
