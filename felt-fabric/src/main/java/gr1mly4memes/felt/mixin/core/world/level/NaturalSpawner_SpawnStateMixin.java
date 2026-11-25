package gr1mly4memes.felt.mixin.core.world.level;

import gr1mly4memes.felt.bridge.core.world.spawner.WorldEntitySpawnerBridge;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NaturalSpawner.SpawnState.class)
public abstract class NaturalSpawner_SpawnStateMixin implements WorldEntitySpawnerBridge.EntityDensityManagerBridge {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "afterSpawn",
        at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/world/level/LocalMobCapCalculator;addMob(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/entity/MobCategory;)V"))
    private void felt$optionalLocalCaps(LocalMobCapCalculator caps, ChunkPos pos, MobCategory category,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (caps != null) original.call(caps, pos, category);
    }

    // @formatter:off
    @Shadow protected abstract void afterSpawn(Mob p_234990_1_, ChunkAccess p_234990_2_);
    @Shadow @Final private int spawnableChunkCount;
    @Shadow @Final private Object2IntOpenHashMap<MobCategory> mobCategoryCounts;
    @Shadow protected abstract boolean canSpawn(EntityType<?> p_234989_1_, Level level, BlockPos p_234989_2_, ChunkAccess p_234989_3_);
    @Shadow protected abstract boolean canSpawnForCategoryLocal(MobCategory category, ChunkPos pos);
    // @formatter:on

    @Override
    public boolean bridge$canSpawn(EntityType<?> entityType, Level level, BlockPos pos, ChunkAccess chunk) {
        return this.canSpawn(entityType, level, pos, chunk);
    }

    @Override
    public void bridge$updateDensity(Mob mobEntity, ChunkAccess chunk) {
        this.afterSpawn(mobEntity, chunk);
    }

    @Override
    public boolean bridge$canSpawn(MobCategory classification, ChunkPos pos, int limit) {
        int i = limit * this.spawnableChunkCount / 289;
        return this.mobCategoryCounts.getInt(classification) < i;
    }

    @Override
    public boolean bridge$canSpawnLocal(MobCategory classification, ChunkPos pos) {
        return this.canSpawnForCategoryLocal(classification, pos);
    }
}
