package gr1mly4memes.felt.bridge.vanilla.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

public interface LivingEntityBridge_Vanilla {
    void felt$vanilla$callLivingDropsEvent(DamageSource source, List<ItemEntity> capturedDrops);
}
