package gr1mly4memes.felt.bridge.core.server.level;

import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelStorageSource;

public interface ServerLevelBridge extends WorldBridge {

    alternate.current.wire.WireHandler bridge$getWireHandler();

    net.minecraft.world.level.storage.PrimaryLevelData bridge$getPrimaryLevelData();

    net.minecraft.world.level.levelgen.WorldGenSettings getWorldGenSettings();

    boolean bridge$createExplosion(Entity source, double x, double y, double z, float power, boolean fire,
                                   net.minecraft.world.level.Level.ExplosionInteraction interaction, boolean excludeSource);

    <T extends ParticleOptions> int bridge$sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force);

    void bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause cause);

    void bridge$strikeLightning(LightningBolt entity, LightningStrikeEvent.Cause cause);

    BlockEntity bridge$getTileEntity(BlockPos blockPos);

    boolean bridge$addEntitySerialized(Entity entity, CreatureSpawnEvent.SpawnReason reason);

    boolean bridge$addAllEntities(Entity entity, CreatureSpawnEvent.SpawnReason reason);

    boolean bridge$addAllEntitiesSafely(Entity entity, CreatureSpawnEvent.SpawnReason reason);

    LevelStorageSource.LevelStorageAccess bridge$getConvertable();
}
