package gr1mly4memes.felt.common.mixin.core;

import gr1mly4memes.felttools.api.FeltVersion;
import gr1mly4memes.felt.common.mod.server.FeltServer;
import net.minecraft.CrashReport;
import net.minecraft.SystemReport;
import org.bukkit.craftbukkit.CraftCrashReport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
public class CrashReportMixin {

    @Shadow @Final private SystemReport systemReport;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$additional(String string, Throwable throwable, CallbackInfo ci) {
        this.systemReport.setDetail("Felt Release", FeltVersion.current()::getReleaseName);
        if (FeltServer.isInitialized()) {
            this.systemReport.setDetail("Felt", new CraftCrashReport());
        } else {
            this.systemReport.setDetail("Felt", "The crash happens before the server initialization.");
        }
    }
}
