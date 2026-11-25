package gr1mly4memes.felt.mixin.core.world.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import gr1mly4memes.felt.mod.server.world.FeltExperience;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ThrownExperienceBottle.class)
public abstract class ThrownExperienceBottleMixin extends ThrowableItemProjectileMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            int i = 3 + this.random.nextInt(5) + this.random.nextInt(5);
            ExpBottleEvent event = CraftEventFactory.callExpBottleEvent((ThrownExperienceBottle) (Object) this, result, i);
            i = event.getExperience();
            if (event.getShowEffect()) {
                this.level().levelEvent(2002, this.blockPosition(), new PotionContents(Potions.WATER).getColor());
            }
            Vec3 direction = result instanceof BlockHitResult blockHitResult
                ? blockHitResult.getDirection().getUnitVec3() : this.getDeltaMovement().scale(-1.0D);
            FeltExperience.award((ServerLevel) this.level(), result.getLocation(), direction, i,
                org.bukkit.entity.ExperienceOrb.SpawnReason.EXP_BOTTLE, this.getOwner(), (ThrownExperienceBottle) (Object) this);
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
            this.discard();
        }
    }
}
