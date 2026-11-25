package gr1mly4memes.felt.mixin.core.world.ticks;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(net.minecraft.world.ticks.LevelChunkTicks.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseLevelChunkTicksMixin implements ca.spottedleaf.moonrise.patches.chunk_system.ticks.ChunkSystemLevelChunkTicks {
    @Shadow @Final private java.util.Queue<?> tickQueue;
    @Unique private boolean felt$dirty;
    @Unique private long felt$lastSaved = Long.MIN_VALUE;
    @Override public boolean moonrise$isDirty(long tick) { return this.felt$dirty || (!this.tickQueue.isEmpty() && tick != this.felt$lastSaved); }
    @Override public void moonrise$clearDirty() { this.felt$dirty = false; }
    @Inject(method = {"schedule", "removeIf"}, at = @At("RETURN"))
    private void felt$dirty(CallbackInfo ci) { this.felt$dirty = true; }
    @Inject(method = "poll", at = @At("RETURN"))
    private void felt$poll(CallbackInfoReturnable<?> cir) { if (cir.getReturnValue() != null) this.felt$dirty = true; }
    @Inject(method = "pack", at = @At("HEAD"))
    private void felt$save(long tick, CallbackInfoReturnable<?> cir) { this.felt$lastSaved = tick; }
    @Inject(method = "unpack", at = @At("RETURN"))
    private void felt$unpack(long tick, CallbackInfo ci) { this.felt$lastSaved = tick; }

}
