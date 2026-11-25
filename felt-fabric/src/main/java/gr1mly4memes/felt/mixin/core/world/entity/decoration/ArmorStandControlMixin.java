package gr1mly4memes.felt.mixin.core.world.entity.decoration;

import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felt.bridge.core.world.entity.decoration.ArmorStandControlBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStand.class)
public abstract class ArmorStandControlMixin extends LivingEntity implements ArmorStandControlBridge {
    protected ArmorStandControlMixin(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }
    @Unique private boolean felt$canMove = true;
    @Unique private boolean felt$canTick = true;
    @Unique private boolean felt$tickOverride;
    @Unique private boolean felt$equipmentDirty;
    @Override public boolean bridge$canMove() { return this.felt$canMove; }
    @Override public void bridge$setCanMove(boolean move) { this.felt$canMove = move; }
    @Override public boolean bridge$canTick() { return this.felt$canTick; }
    @Override public void bridge$setCanTick(boolean tick) { this.felt$canTick = tick; this.felt$tickOverride = true; }
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void felt$initialTickConfiguration(EntityType<? extends ArmorStand> type, Level level, CallbackInfo ci) {
        if (level instanceof ServerLevel && ((WorldBridge)level).bridge$paperConfig() != null)
            this.felt$canTick = ((WorldBridge)level).bridge$paperConfig().entities.armorStands.tick;
    }
    @Override public void tick() {
        if (this.felt$canTick) { super.tick(); return; }
        if (this.felt$equipmentDirty) {
            this.felt$equipmentDirty = false;
            ((LivingEntityBridge)this).detectEquipmentUpdatesPublic();
        }
        if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) this.tickDeath();
    }
    @Override public void move(MoverType type, Vec3 movement) {
        if (this.felt$canMove) super.move(type, movement);
    }
    @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        this.felt$equipmentDirty = true;
    }
    @Override public void bridge$equipmentChanged() { this.felt$equipmentDirty = true; }
    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void felt$saveTickOverride(ValueOutput output, CallbackInfo ci) {
        if (this.felt$tickOverride) output.putBoolean("Paper.CanTickOverride", this.felt$canTick);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void felt$loadTickOverride(ValueInput input, CallbackInfo ci) {
        if (input.getInt("Paper.CanTickOverride").isPresent()) this.bridge$setCanTick(input.getBooleanOr("Paper.CanTickOverride", true));
    }
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void felt$collisionLookupConfiguration(CallbackInfo ci) {
        if (this.level() instanceof ServerLevel && !((WorldBridge)this.level()).bridge$paperConfig().entities.armorStands.doCollisionEntityLookups) ci.cancel();
    }
}
