package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.world.entity.ItemBasedSteeringBridge;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ItemBasedSteering;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemBasedSteering.class)
public class ItemBasedSteeringMixin implements ItemBasedSteeringBridge {

    // @formatter:off
    @Shadow public boolean boosting;
    @Shadow public int boostTime;
    @Shadow @Final private SynchedEntityData entityData;
    @Shadow @Final private EntityDataAccessor<Integer> boostTimeAccessor;
    // @formatter:on

    @Override
    public void setBoostTicks(int ticks) {
        this.boosting = true;
        this.boostTime = 0;
        this.entityData.set(this.boostTimeAccessor, ticks);
    }
}
