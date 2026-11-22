package gr1mly4memes.felt.mixin.core.world.level.portal;

import gr1mly4memes.felt.bridge.core.world.level.portal.DimensionTransitionBridge;
import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TeleportTransition.class)
public class DimensionTransitionMixin implements DimensionTransitionBridge {

    @ShadowConstructor
    public void felt$constructor(ServerLevel newLevel, Vec3 pos, Vec3 speed, float yRot, float xRot, boolean missingRespawnBlock, boolean asPassenger, java.util.Set<net.minecraft.world.entity.Relative> relatives, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void felt$constructor(ServerLevel newLevel, Vec3 pos, Vec3 speed, float yRot, float xRot, boolean missingRespawnBlock, TeleportTransition.PostTeleportTransition postTeleportTransition, PlayerTeleportEvent.TeleportCause cause) {
        felt$constructor(newLevel, pos, speed, yRot, xRot, missingRespawnBlock, false, java.util.Set.of(), postTeleportTransition);
        this.cause = cause;
    }

    @ShadowConstructor
    public void felt$constructor(ServerLevel serverLevel, Vec3 vec3, Vec3 vec32, float f, float g, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void felt$constructor(ServerLevel serverLevel, Vec3 vec3, Vec3 vec32, float f, float g, TeleportTransition.PostTeleportTransition postTeleportTransition, PlayerTeleportEvent.TeleportCause cause) {
        felt$constructor(serverLevel, vec3, vec32, f, g, postTeleportTransition);
        this.cause = cause;
    }

    @Unique private PlayerTeleportEvent.TeleportCause cause; // Paper added

    @Unique private boolean felt$positionRider;

    @Override
    public void bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause cause) {
        this.cause = cause;
    }

    @Override
    public PlayerTeleportEvent.TeleportCause bridge$getTeleportCause() {
        return this.cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : this.cause;
    }

    @Override
    public void bridge$setPositionRider(boolean value) {
        this.felt$positionRider = value;
    }

    @Override
    public boolean bridge$isPositionRider() {
        return this.felt$positionRider;
    }

    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = {
        "withRotation(FF)Lnet/minecraft/world/level/portal/TeleportTransition;",
        "withPosition(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/level/portal/TeleportTransition;",
        "transitionAsPassenger()Lnet/minecraft/world/level/portal/TeleportTransition;"
    }, at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private TeleportTransition felt$copyMetadata(TeleportTransition result) {
        var bridge = (DimensionTransitionBridge) (Object) result;
        bridge.bridge$setTeleportCause(this.cause);
        bridge.bridge$setPositionRider(this.felt$positionRider);
        return result;
    }
}
