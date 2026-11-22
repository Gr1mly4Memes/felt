package gr1mly4memes.felt.mixin.core.server;

import gr1mly4memes.felt.bridge.core.server.level.ServerLevelSavedDataBridge;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fixed CraftBukkit exposes overworld's SavedData through the server-wide API.
 * Until the overworld has been installed, retain native global getters during init. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerSavedDataMixin {

    @Shadow public abstract ServerLevel overworld();

    @Inject(method = "getCustomBossEvents", at = @At("HEAD"), cancellable = true)
    private void felt$overworldBossEvents(CallbackInfoReturnable<CustomBossEvents> cir) {
        ServerLevel overworld = this.overworld();
        if (overworld != null) {
            cir.setReturnValue(((ServerLevelSavedDataBridge) overworld).bridge$getCustomBossEvents());
        }
    }

    @Inject(method = "getRandomSequences", at = @At("HEAD"), cancellable = true)
    private void felt$overworldRandomSequences(CallbackInfoReturnable<RandomSequences> cir) {
        ServerLevel overworld = this.overworld();
        if (overworld != null) {
            cir.setReturnValue(((ServerLevelSavedDataBridge) overworld).bridge$getRandomSequences());
        }
    }

    @Inject(method = "getRandomSequence", at = @At("HEAD"), cancellable = true)
    private void felt$overworldRandomSequence(Identifier key, CallbackInfoReturnable<RandomSource> cir) {
        ServerLevel overworld = this.overworld();
        if (overworld != null) {
            cir.setReturnValue(((ServerLevelSavedDataBridge) overworld).bridge$getRandomSequence(key));
        }
    }
}
