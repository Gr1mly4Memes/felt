package gr1mly4memes.felt.mixin.core.world.entity.boss.enderdragon.phases;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gr1mly4memes.felt.bridge.core.world.entity.EnderDragonPodiumBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonDeathPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingApproachPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonTakeoffPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({DragonDeathPhase.class, DragonHoldingPatternPhase.class, DragonLandingApproachPhase.class, DragonLandingPhase.class, DragonTakeoffPhase.class})
public abstract class DragonPodiumPhaseMixin extends AbstractDragonPhaseInstance {
    protected DragonPodiumPhaseMixin(EnderDragon dragon) {
        super(dragon);
    }

    @ModifyExpressionValue(method = {"doServerTick", "findNewTarget"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;getPodiumLocation(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"),
        require = 1, allow = 1)
    private BlockPos felt$useDragonPodium(BlockPos defaultPodium) {
        return ((EnderDragonPodiumBridge) this.dragon).bridge$getPodium();
    }
}
