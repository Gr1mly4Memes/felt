package gr1mly4memes.felt.mixin.core.world.level;

import gr1mly4memes.felt.bridge.core.world.level.IWorldWriterBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelWriter;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelWriter.class)
public interface LevelWriterMixin extends IWorldWriterBridge {

    default boolean addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return bridge$addEntity(entity, reason);
    }
}
