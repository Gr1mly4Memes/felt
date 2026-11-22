package gr1mly4memes.felt.bridge.bukkit.world.entity;

import gr1mly4memes.felt.mod.config.EntityPropertySpec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Location;

import java.util.function.Function;

public interface EntityTypeBridge {

    void bridge$setup(Identifier location, EntityType<?> entityType, EntityPropertySpec spec);

    EntityType<?> bridge$getHandle();

    void bridge$setHandle(EntityType<?> entityType);

    EntityPropertySpec bridge$getSpec();

    Function<Location, ? extends Entity> bridge$entityFactory();

    void bridge$setEntityFactory(Function<Location, ? extends Entity> function);
}
