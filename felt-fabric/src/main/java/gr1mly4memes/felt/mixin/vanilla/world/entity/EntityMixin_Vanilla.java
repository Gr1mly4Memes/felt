package gr1mly4memes.felt.mixin.vanilla.world.entity;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.entity.InternalEntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin_Vanilla implements InternalEntityBridge, EntityBridge {

    // @formatter:off
    @Shadow public abstract int getId();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract void discard();
    @Shadow public abstract Level level();
    @Shadow public abstract EntityType<?> getType();
    @Shadow public abstract double getX();
    @Shadow public abstract double getY();
    @Shadow public abstract double getZ();
    // @formatter:on

    @Unique private List<ItemEntity> felt$capturedDrops;

    @Unique
    public void felt$startCaptureDrops() {
        felt$capturedDrops = new ArrayList<>();
    }

    @Unique
    public boolean felt$captureDrop(ItemEntity itemEntity) {
        if (felt$capturedDrops != null) {
            felt$capturedDrops.add(itemEntity);
            return true;
        }
        return false;
    }

    @Unique
    public List<ItemEntity> felt$finishCaptureDrops() {
        final var drops = felt$capturedDrops;
        felt$capturedDrops = null;
        return drops;
    }

    @Decorate(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean felt$captureEntityDrops(ServerLevel instance, Entity entity) throws Throwable {
        if (!bridge$isForceDrops() && felt$captureDrop((ItemEntity) entity)) {
            return true;
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }
}
