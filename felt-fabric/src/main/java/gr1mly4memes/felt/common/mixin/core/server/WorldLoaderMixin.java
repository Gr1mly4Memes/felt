package gr1mly4memes.felt.common.mixin.core.server;

import gr1mly4memes.felt.common.mod.util.FeltCaptures;
import net.minecraft.server.WorldLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {

    @ModifyArg(method = "lambda$load$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/WorldLoader$WorldDataSupplier;get(Lnet/minecraft/server/WorldLoader$DataLoadContext;)Lnet/minecraft/server/WorldLoader$DataLoadOutput;"))
    private static WorldLoader.DataLoadContext arclight$captureContext(WorldLoader.DataLoadContext context) {
        FeltCaptures.captureDataLoadContext(context);
        return context;
    }
}
