package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import io.papermc.paper.event.entity.EntityConstructEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherSkullBlock.class)
public class WitherSkullBlockMixin {

    @Redirect(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CarvedPumpkinBlock;clearPatternBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;)V"))
    private static void felt$clearLater(Level p_249604_, BlockPattern.BlockPatternMatch p_251190_) {
    }

    @Decorate(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean felt$muteSpawn(Level instance, Entity entity, @Local(ordinal = -1) BlockPattern.BlockPatternMatch patternMatch) throws Throwable {
        if (!new EntityConstructEvent(entity.bridge$getBukkitEntity(), CraftBlock.getMatchingBlocks(instance, patternMatch)).callEvent()) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        ((WorldBridge) instance).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BUILD_WITHER);
        if (!(boolean) DecorationOps.callsite().invoke(instance, entity)) {
            return (boolean) DecorationOps.cancel().invoke();
        } else {
            CarvedPumpkinBlock.clearPatternBlocks(instance, patternMatch);
        }
        return true;
    }
}
