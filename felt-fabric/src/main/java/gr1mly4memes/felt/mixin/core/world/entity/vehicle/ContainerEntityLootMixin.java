package gr1mly4memes.felt.mixin.core.world.entity.vehicle;

import com.destroystokyo.paper.loottable.PaperLootableInventoryData;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.world.LootableDataBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ContainerEntity.class)
public interface ContainerEntityLootMixin {
    @WrapMethod(method = "addChestVehicleSaveData")
    private void felt$saveLootAndItems(ValueOutput output, Operation<Void> original) {
        original.call(output);
        var vehicle = (ContainerEntity)this;
        if (vehicle.getContainerLootTable() != null) {
            ((LootableDataBridge)this).bridge$lootableData().saveNbt(output);
            ContainerHelper.saveAllItems(output, vehicle.getItemStacks());
        }
    }
    @WrapMethod(method = "readChestVehicleSaveData")
    private void felt$readLootAndItems(ValueInput input, Operation<Void> original) {
        original.call(input);
        var vehicle = (ContainerEntity)this;
        if (vehicle.getContainerLootTable() != null) {
            ((LootableDataBridge)this).bridge$lootableData().loadNbt(input);
            ContainerHelper.loadAllItems(input, vehicle.getItemStacks());
        }
    }
    @WrapMethod(method = "unpackChestVehicleLootTable")
    private void felt$refillOnlyWhenDue(Player player, Operation<Void> original) {
        var vehicle = (ContainerEntity)this;
        if (vehicle.level().getServer() == null || ((LootableDataBridge)this).bridge$lootableData().shouldReplenish(vehicle, PaperLootableInventoryData.ENTITY, player)) original.call(player);
    }
    @WrapOperation(method = "unpackChestVehicleLootTable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/ContainerEntity;setContainerLootTable(Lnet/minecraft/resources/ResourceKey;)V"))
    private void felt$retainVehicleLootTable(ContainerEntity vehicle, ResourceKey<LootTable> table, Operation<Void> original, Player player) {
        if (((LootableDataBridge)this).bridge$lootableData().shouldClearLootTable(vehicle, PaperLootableInventoryData.ENTITY, player)) original.call(vehicle, table);
    }
}
