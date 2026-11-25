package gr1mly4memes.felt.mixin.core.world.level.block.entity;

import com.destroystokyo.paper.loottable.PaperLootableInventoryData;
import gr1mly4memes.felt.bridge.core.world.LootableDataBridge;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerDataMixin implements LootableDataBridge {
    @Unique private final PaperLootableInventoryData felt$lootableData = new PaperLootableInventoryData();
    @Override public PaperLootableInventoryData bridge$lootableData() { return this.felt$lootableData; }
}
