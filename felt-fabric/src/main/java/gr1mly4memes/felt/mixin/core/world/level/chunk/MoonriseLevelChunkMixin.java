package gr1mly4memes.felt.mixin.core.world.level.chunk;

import ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkSystemLevelChunk;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseLevelChunkMixin implements ChunkSystemLevelChunk {
    @Unique private boolean felt$postProcessingDone;
    @Unique private NewChunkHolder felt$nativeHolder;
    @Override public boolean moonrise$isPostProcessingDone() { return this.felt$postProcessingDone; }
    @Override public NewChunkHolder moonrise$getChunkHolder() { return this.felt$nativeHolder; }
    @Override public void moonrise$setChunkHolder(NewChunkHolder holder) { this.felt$nativeHolder = holder; }
    @Inject(method = "postProcessGeneration", at = @At("RETURN"))
    private void felt$postProcessed(CallbackInfo ci) { this.felt$postProcessingDone = true; }
    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void felt$copyLight(ServerLevel world, ProtoChunk proto, LevelChunk.PostLoadProcessor processor, CallbackInfo ci) {
        StarlightChunk from = (StarlightChunk) proto, into = (StarlightChunk) this;
        into.starlight$setBlockNibbles(from.starlight$getBlockNibbles());
        into.starlight$setSkyNibbles(from.starlight$getSkyNibbles());
        into.starlight$setBlockEmptinessMap(from.starlight$getBlockEmptinessMap());
        into.starlight$setSkyEmptinessMap(from.starlight$getSkyEmptinessMap());
    }
}
