package gr1mly4memes.felt.bridge.core.world.level;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CapturedBlockState;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.generator.ChunkGenerator;
import org.spigotmc.SpigotWorldConfig;

import java.util.Map;

public interface WorldBridge extends IWorldWriterBridge, LevelAccessorBridge {

    int bridge$getWakeupInactiveAnimals();
    void bridge$setWakeupInactiveAnimals(int value);
    int bridge$getWakeupInactiveFlying();
    void bridge$setWakeupInactiveFlying(int value);
    int bridge$getWakeupInactiveMonsters();
    void bridge$setWakeupInactiveMonsters(int value);
    int bridge$getWakeupInactiveVillagers();
    void bridge$setWakeupInactiveVillagers(int value);


    default CraftWorld getWorld() {
        return bridge$getWorld();
    }

    default CraftServer getCraftServer() {
        return bridge$getServer();
    }

    default BlockEntity getBlockEntity(BlockPos pos, boolean validate) {
        return bridge$getTileEntity(pos, validate);
    }

    CraftServer bridge$getServer();

    CraftWorld bridge$getWorld();

    boolean bridge$isPvpMode();

    boolean bridge$isPopulating();

    void bridge$setPopulating(boolean populating);

    ChunkGenerator bridge$getGenerator();

    BlockEntity bridge$getTileEntity(BlockPos pos, boolean validate);

    SpigotWorldConfig bridge$spigotConfig();

    io.papermc.paper.configuration.WorldConfiguration bridge$paperConfig();

    Object2LongOpenHashMap<SpawnCategory> bridge$ticksPerSpawnCategory();

    ResourceKey<LevelStem> bridge$getTypeKey();

    void bridge$setLastPhysicsProblem(BlockPos pos);

    boolean bridge$preventPoiUpdated();

    void bridge$preventPoiUpdated(boolean b);

    void bridge$mod$notifyAndUpdatePhysics(BlockPos pos, LevelChunk chunk, BlockState oldBlock, BlockState newBlock, int i, int j);

    default boolean bridge$mod$mobGriefing(Entity entity) {
        return this instanceof ServerLevel level && level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    Map<BlockPos, CapturedBlockState> bridge$getCapturedBlockState();

    Map<BlockPos, BlockEntity> bridge$getCapturedBlockEntity();

    default void bridge$platform$startCaptureBlockBreak() {}

    default boolean bridge$isCapturingBlockBreak() {
        return false;
    }

    default void bridge$platform$endCaptureBlockBreak() {}

    default io.papermc.paper.configuration.WorldConfiguration paperConfig() {
        return this.bridge$paperConfig();
    }

    default ResourceKey<LevelStem> getTypeKey() {
        return this.bridge$getTypeKey();
    }

}
