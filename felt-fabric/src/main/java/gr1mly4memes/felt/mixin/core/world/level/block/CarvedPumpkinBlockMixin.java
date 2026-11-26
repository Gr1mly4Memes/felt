package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import io.papermc.paper.event.entity.EntityConstructEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinBlockMixin {

    @Redirect(method = "spawnGolemInWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CarvedPumpkinBlock;clearPatternBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;)V"))
    private static void felt$clearLater(Level level, BlockPattern.BlockPatternMatch match) {
    }

    @Inject(method = "spawnGolemInWorld", cancellable = true, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void felt$constructGolem(Level level, BlockPattern.BlockPatternMatch match, Entity entity, BlockPos pos, CallbackInfo ci) {
        if (!new EntityConstructEvent(entity.bridge$getBukkitEntity(), CraftBlock.getMatchingBlocks(level, match)).callEvent()) {
            ci.cancel();
            return;
        }
        var type = entity.getType();
        var reason = type == EntityTypes.SNOW_GOLEM ? CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN
            : type == EntityTypes.COPPER_GOLEM ? CreatureSpawnEvent.SpawnReason.BUILD_COPPERGOLEM : CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM;
        ((WorldBridge) level).bridge$pushAddEntityReason(reason);
    }

    @Inject(method = "spawnGolemInWorld", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void felt$clearPattern(Level p_249110_, BlockPattern.BlockPatternMatch p_251293_, Entity entity, BlockPos p_251189_, CallbackInfo ci) {
        if (!((EntityBridge) entity).bridge$isValid()) {
            ci.cancel();
        } else {
            CarvedPumpkinBlock.clearPatternBlocks(p_249110_, p_251293_);
        }
    }
}
