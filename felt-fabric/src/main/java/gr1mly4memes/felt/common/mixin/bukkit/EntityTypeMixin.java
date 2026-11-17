package gr1mly4memes.felt.common.mixin.bukkit;

import gr1mly4memes.felt.common.bridge.bukkit.world.entity.EntityTypeBridge;
import gr1mly4memes.felt.common.mod.server.FeltServer;
import gr1mly4memes.felt.i18n.conf.EntityPropertySpec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Function;

@Mixin(value = EntityType.class, remap = false)
public class EntityTypeMixin implements EntityTypeBridge {

    // @formatter:off
    @Shadow @Final @Mutable private NamespacedKey key;
    @Shadow @Final @Mutable private Class<? extends Entity> clazz;
    @Shadow @Final @Mutable private String name;
    // @formatter:on

    private net.minecraft.world.entity.EntityType<?> handleType;
    private EntityPropertySpec spec;
    private Function<Location, ? extends net.minecraft.world.entity.Entity> factory;

    @Override
    public void bridge$setup(Identifier location, net.minecraft.world.entity.EntityType<?> entityType, EntityPropertySpec spec) {
        this.key = CraftNamespacedKey.fromMinecraft(location);
        this.name = location.toString();
        this.handleType = entityType;
        this.spec = spec.clone();
        this.setup();
    }

    @SuppressWarnings("unchecked")
    private void setup() {
        if (this.spec.entityClass != null) {
            try {
                Class<?> cl = Class.forName(this.spec.entityClass);
                if (!Entity.class.isAssignableFrom(cl)) {
                    FeltServer.LOGGER.warn("{} is not a subclass of {}", cl, Entity.class);
                } else {
                    this.clazz = (Class<? extends Entity>) cl;
                }
            } catch (Exception e) {
                FeltServer.LOGGER.warn("{} provided entityClass {} is invalid: {}", this, this.spec.entityClass, e);
            }
        }
        this.factory = loc -> {
            if (loc != null && loc.getWorld() != null) {
                ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
                net.minecraft.world.entity.Entity entity = handleType.create(world, EntitySpawnReason.COMMAND);
                if (entity != null) {
                    entity.snapTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                }
                if (entity instanceof LivingEntity) {
                    entity.setYHeadRot(loc.getYaw());
                    entity.setYBodyRot(loc.getYaw());
                }
                return entity;
            } else return null;
        };
    }

    @Override
    public net.minecraft.world.entity.EntityType<?> bridge$getHandle() {
        return this.handleType;
    }

    @Override
    public void bridge$setHandle(net.minecraft.world.entity.EntityType<?> entityType) {
        this.handleType = entityType;
    }

    @Override
    public EntityPropertySpec bridge$getSpec() {
        return spec;
    }

    @Override
    public Function<Location, ? extends net.minecraft.world.entity.Entity> bridge$entityFactory() {
        return factory;
    }

    @Override
    public void bridge$setEntityFactory(Function<Location, ? extends net.minecraft.world.entity.Entity> function) {
        this.factory = function;
    }
}
