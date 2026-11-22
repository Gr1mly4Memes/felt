package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.bridge.core.world.damagesource.DamageSourceBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {

    @Decorate(method = "destroyOnUse", inject = true, require = 1, allow = 1, at = @At("HEAD"))
    private void felt$captureBed(BlockState state, Level level, BlockPos pos, Player player,
            @Local(allocate = "feltBedSnapshot") org.bukkit.block.BlockState snapshot) throws Throwable {
        snapshot = CraftBlock.at(level, pos).getState();
        DecorationOps.blackhole().invoke(snapshot);
    }

    @Decorate(method = "destroyOnUse", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;badRespawnPointExplosion(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource felt$bedDamageSource(DamageSources sources, Vec3 pos,
            @Local(allocate = "feltBedSnapshot") org.bukkit.block.BlockState snapshot) throws Throwable {
        DamageSource source = (DamageSource) DecorationOps.callsite().invoke(sources, pos);
        return ((DamageSourceBridge) source).bridge$directBlockState(snapshot);
    }
}
