package gr1mly4memes.felt.mixin.core.world.entity.animal.cow;

import gr1mly4memes.felt.mixin.core.world.entity.animal.AnimalMixin;
import gr1mly4memes.felt.mod.server.event.FeltShearing;
import gr1mly4memes.felt.mod.server.event.FeltEventFactory;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin extends AnimalMixin {

    @Decorate(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void felt$onShear(MushroomCow cow, ServerLevel level, SoundSource source, ItemStack tool, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaultDrops = FeltShearing.defaultDrops(cow, level, tool);
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, cow, tool, hand, defaultDrops);
        if (event != null && event.isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }

        List<ItemStack> drops = event == null ? defaultDrops : CraftItemStack.asNMSCopy(event.getDrops());
        var previous = FeltShearing.captureDrops(cow, tool, drops);
        try {
            DecorationOps.callsite().invoke(cow, level, source, tool);
        } finally {
            FeltShearing.restoreDrops(previous);
        }
    }

    @Decorate(method = {"lambda$shear$0", "lambda$shear$1"}, require = 1, allow = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemInstance;Ljava/util/function/BiConsumer;)V"))
    private void felt$dropShearedItems(MushroomCow cow, ServerLevel level, ResourceKey<LootTable> lootTable, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> dropper) throws Throwable {
        List<ItemStack> drops = BuiltInLootTables.SHEAR_MOOSHROOM.equals(lootTable)
            ? FeltShearing.takeDrops(cow, tool) : null;
        boolean previousForceDrops = this.bridge$isForceDrops();
        this.bridge$setForceDrops(true);
        try {
            if (drops == null) {
                DecorationOps.callsite().invoke(cow, level, lootTable, tool, dropper);
            } else {
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        var item = new ItemEntity(level, cow.getX(), cow.getY(1.0), cow.getZ(), drop);
                        if (FeltEventFactory.callEntityDropItemEvent(cow, item)) {
                            level.addFreshEntity(item);
                        }
                    }
                }
            }
        } finally {
            this.bridge$setForceDrops(previousForceDrops);
        }
    }
}
