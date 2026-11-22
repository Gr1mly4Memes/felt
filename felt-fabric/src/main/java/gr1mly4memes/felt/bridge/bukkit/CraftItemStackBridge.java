package gr1mly4memes.felt.bridge.bukkit;

import net.minecraft.world.entity.item.ItemEntity;

public interface CraftItemStackBridge {
    void felt$setItemEntity(ItemEntity entity);
    ItemEntity felt$getItemEntity();
}
