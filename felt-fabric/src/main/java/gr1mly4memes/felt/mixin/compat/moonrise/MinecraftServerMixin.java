package gr1mly4memes.felt.mixin.compat.moonrise;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.PRESENT)
public abstract class MinecraftServerMixin {
    @WrapMethod(method = "felt$tickPendingEntities", remap = false)
    private void felt$skipLegacyEntityManager(ServerLevel level, Operation<Void> original) {
    }
}
