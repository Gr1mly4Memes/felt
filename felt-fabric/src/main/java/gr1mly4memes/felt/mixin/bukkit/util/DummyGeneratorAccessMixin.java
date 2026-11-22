package gr1mly4memes.felt.mixin.bukkit.util;

import gr1mly4memes.felt.bridge.core.world.level.IWorldWriterBridge;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.util.DummyLevelAccessor;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DummyLevelAccessor.class)
public class DummyGeneratorAccessMixin implements IWorldWriterBridge {
    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        // DummyLevelAccessor has no real world to add entities to; report failure so callers handle it.
        return false;
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        // No-op: there is no spawn-reason tracking on the dummy accessor.
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        return null;
    }
}
