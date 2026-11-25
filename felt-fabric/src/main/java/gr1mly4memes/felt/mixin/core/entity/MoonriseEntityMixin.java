package gr1mly4memes.felt.mixin.core.entity;

import ca.spottedleaf.moonrise.patches.chunk_system.entity.ChunkSystemEntity;
import ca.spottedleaf.moonrise.patches.chunk_system.level.chunk.ChunkData;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseEntityMixin implements ChunkSystemEntity {
    @Shadow private java.util.stream.Stream<Entity> getIndirectPassengersStream() { throw new AssertionError(); }
    @Unique private final boolean felt$hardColliding = this.moonrise$isHardCollidingUncached();
    @Unique private FullChunkStatus felt$chunkStatus;
    @Unique private ChunkData felt$chunkData;
    @Unique private int felt$sectionX = Integer.MIN_VALUE, felt$sectionY = Integer.MIN_VALUE, felt$sectionZ = Integer.MIN_VALUE;
    @Unique private boolean felt$updatingSection;
    @Override public boolean moonrise$isHardColliding() { return this.felt$hardColliding; }
    @Override public FullChunkStatus moonrise$getChunkStatus() { return this.felt$chunkStatus; }
    @Override public void moonrise$setChunkStatus(FullChunkStatus status) { this.felt$chunkStatus = status; }
    @Override public ChunkData moonrise$getChunkData() { return this.felt$chunkData; }
    @Override public void moonrise$setChunkData(ChunkData data) { this.felt$chunkData = data; }
    @Override public int moonrise$getSectionX() { return this.felt$sectionX; }
    @Override public int moonrise$getSectionY() { return this.felt$sectionY; }
    @Override public int moonrise$getSectionZ() { return this.felt$sectionZ; }
    @Override public void moonrise$setSectionX(int x) { this.felt$sectionX = x; }
    @Override public void moonrise$setSectionY(int y) { this.felt$sectionY = y; }
    @Override public void moonrise$setSectionZ(int z) { this.felt$sectionZ = z; }
    @Override public boolean moonrise$isUpdatingSectionStatus() { return this.felt$updatingSection; }
    @Override public void moonrise$setUpdatingSectionStatus(boolean value) { this.felt$updatingSection = value; }
    @Override public boolean moonrise$hasAnyPlayerPassengers() {
        return this.getIndirectPassengersStream().anyMatch(entity -> entity instanceof Player);
    }
}
