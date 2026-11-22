package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.bridge.core.world.entity.MobBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionTracker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ConversionTracker.class)
public abstract class ConversionTrackerMixin {

    @Shadow @Final private int totalConversionTime;
    @Shadow private int conversionTime;

    @Decorate(method = "doConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob felt$conversion(Mob mob, EntityType<?> type, ConversionParams params, ConversionParams.AfterConversion<?> afterConversion) throws Throwable {
        MobBridge bridge = (MobBridge) mob;
        WorldBridge world = (WorldBridge) mob.level();
        var previousTransform = bridge.bridge$getTransformReason();
        var previousSpawn = world.bridge$getAddEntityReason();
        var transformReason = EntityTransformEvent.TransformReason.UNKNOWN;
        var spawnReason = CreatureSpawnEvent.SpawnReason.DEFAULT;
        if (mob instanceof Zombie zombie && zombie.drowningTracker == (Object) this) {
            transformReason = EntityTransformEvent.TransformReason.DROWNED;
            spawnReason = CreatureSpawnEvent.SpawnReason.DROWNED;
        } else if (mob instanceof Skeleton skeleton && skeleton.freezingTracker == (Object) this) {
            transformReason = EntityTransformEvent.TransformReason.FROZEN;
            spawnReason = CreatureSpawnEvent.SpawnReason.FROZEN;
        }
        // Loader conversion vetoes remain in the native tracker before this call.
        bridge.bridge$pushTransformReason(transformReason);
        world.bridge$pushAddEntityReason(spawnReason);
        try {
            Mob converted = (Mob) DecorationOps.callsite().invoke(mob, type, params, afterConversion);
            if (converted == null) {
                this.conversionTime = this.totalConversionTime;
            }
            return converted;
        } finally {
            bridge.bridge$pushTransformReason(previousTransform);
            world.bridge$pushAddEntityReason(previousSpawn);
        }
    }
}
