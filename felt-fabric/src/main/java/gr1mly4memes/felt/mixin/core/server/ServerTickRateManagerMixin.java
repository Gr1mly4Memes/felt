package gr1mly4memes.felt.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.bridge.core.server.ServerTickRateManagerBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ServerTickRateManager.class)
public abstract class ServerTickRateManagerMixin implements ServerTickRateManagerBridge {

    // @formatter:off
    @Shadow public abstract boolean isSprinting();
    @Shadow public abstract boolean requestGameToSprint(int time);
    @Shadow public abstract boolean stopSprinting();
    // @formatter:on

    private boolean felt$sendLog = true;
    private boolean silent; // Paper added

    @Override
    public boolean bridge$requestGameToSprint(int time, boolean silent) {
        boolean sprinting = this.isSprinting();
        boolean interrupted = this.requestGameToSprint(time);
        if (!sprinting) {
            this.silent = silent;
        }
        return interrupted;
    }

    @WrapMethod(method = "requestGameToSprint(I)Z")
    private boolean felt$requestSprint(int time, Operation<Boolean> original) {
        boolean sprinting = this.isSprinting();
        boolean silent = this.silent;
        boolean interrupted = original.call(time);
        // Keep any old-session report in the native request on its original flag.
        // A direct one-argument request starts a non-silent session by default.
        this.silent = sprinting && silent;
        return interrupted;
    }

    @Inject(method = "finishTickSprint()V", at = @At("RETURN"))
    private void felt$finishSprint(CallbackInfo ci) {
        this.silent = false;
    }

    public boolean stopSprinting(boolean sendLog) {
        try {
            felt$sendLog = sendLog;
            return this.stopSprinting();
        } finally {
            felt$sendLog = true;
        }
    }

    @Redirect(method = "finishTickSprint", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Ljava/util/function/Supplier;Z)V"))
    private void felt$send(CommandSourceStack source, Supplier<Component> supplier, boolean flag) {
        if (this.felt$sendLog && !this.silent) {
            source.sendSuccess(supplier, flag);
        }
    }

    public boolean requestGameToSprint(int time, boolean silent) {
        return this.bridge$requestGameToSprint(time, silent);
    }

}
