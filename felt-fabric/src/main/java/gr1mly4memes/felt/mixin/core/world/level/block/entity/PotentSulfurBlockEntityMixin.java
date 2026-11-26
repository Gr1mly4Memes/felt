package gr1mly4memes.felt.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.papermc.paper.event.entity.EntityPotentSulfurLaunchEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.craftbukkit.util.CraftVector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PotentSulfurBlockEntity.class)
public abstract class PotentSulfurBlockEntityMixin {
    @WrapOperation(method = "lambda$static$5", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static void felt$onGeyserLaunch(Entity entity, Vec3 impulse, Operation<Void> original,
                                            @Local(argsOnly = true) Level level,
                                            @Local(argsOnly = true) BlockPos pos,
                                            @Local(index = 4) BlockPos waterSurface) {
        if (level instanceof ServerLevel world && EntityPotentSulfurLaunchEvent.getHandlerList().hasListeners()) {
            new EntityPotentSulfurLaunchEvent(entity.bridge$getBukkitEntity(), CraftBlock.at(world, pos),
                CraftLocation.toBukkit(waterSurface, world.bridge$getWorld()), CraftVector.toBukkit(impulse)).callEvent();
        }
        original.call(entity, impulse);
    }
}
