package gr1mly4memes.felt.mixin.bukkit;

import gr1mly4memes.felt.bridge.core.world.entity.BeeBridge;
import net.minecraft.world.entity.animal.bee.Bee;
import org.bukkit.craftbukkit.entity.CraftBee;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftBee.class, remap = false)
public abstract class CraftBeeMixin {

    @Redirect(method = "setHasNectar(Z)V", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;setHasNectar(Z)V", remap = true))
    private void felt$setHasNectar(Bee instance, boolean nectar) {
        ((BeeBridge) instance).bridge$setHasNectar(nectar);
    }

    @Redirect(method = "setHasStung(Z)V", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;setHasStung(Z)V", remap = true))
    private void felt$setHasStung(Bee instance, boolean stung) {
        ((BeeBridge) instance).bridge$setHasStung(stung);
    }

    @Redirect(method = "getCannotEnterHiveTicks()I", remap = false, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/entity/animal/bee/Bee;stayOutOfHiveCountdown:I", remap = true))
    private int felt$getCannotEnterHiveTicks(Bee instance) {
        return ((BeeBridge) instance).bridge$getCannotEnterHiveTicks();
    }
}
