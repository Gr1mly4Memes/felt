package gr1mly4memes.felt.mixin.core.network.protocol.game;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientboundAddEntityPacket.class)
public abstract class ClientboundAddEntityPacketMixin {
    @WrapOperation(method = "<init>(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/server/level/ServerEntity;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity;getPositionBase()Lnet/minecraft/world/phys/Vec3;"),
        require = 3, allow = 3)
    private static Vec3 felt$useLiveTrackingPosition(ServerEntity tracker, Operation<Vec3> original,
                                                      @Local(argsOnly = true) Entity entity) {
        return entity.trackingPosition();
    }
}
