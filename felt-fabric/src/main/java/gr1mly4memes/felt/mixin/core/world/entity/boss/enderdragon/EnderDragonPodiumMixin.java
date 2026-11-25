package gr1mly4memes.felt.mixin.core.world.entity.boss.enderdragon;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gr1mly4memes.felt.bridge.core.world.entity.EnderDragonPodiumBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderDragon.class)
public abstract class EnderDragonPodiumMixin implements EnderDragonPodiumBridge {
    @Unique private BlockPos felt$podium;
    @Shadow public abstract BlockPos getFightOrigin();

    @Override
    public BlockPos bridge$getPodium() {
        return this.felt$podium == null ? EnderDragonFight.getPodiumLocation(this.getFightOrigin()) : this.felt$podium;
    }

    @Override
    public void bridge$setPodium(BlockPos podium) {
        this.felt$podium = podium == null ? null : podium.immutable();
    }

    @ModifyExpressionValue(method = "getHeadLookVector", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;getPodiumLocation(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos felt$lookAtPodium(BlockPos defaultPodium) {
        return this.felt$podium == null ? defaultPodium : this.felt$podium;
    }
}
