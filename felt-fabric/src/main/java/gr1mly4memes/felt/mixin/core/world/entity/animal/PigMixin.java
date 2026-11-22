package gr1mly4memes.felt.mixin.core.world.entity.animal;

import gr1mly4memes.felt.bridge.core.world.entity.animal.PigBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.pig.Pig;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Pig.class)
public abstract class PigMixin extends AnimalMixin implements PigBridge {

    @Decorate(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/pig/Pig;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob felt$pigZap(Pig pig, EntityType<? extends Mob> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion afterConversion, ServerLevel world, LightningBolt lightningBolt) throws Throwable {
        this.felt$lightningBolt = lightningBolt;
        this.felt$pigZapCancelled = false;
        Mob converted;
        boolean cancelled;
        try {
            ((WorldBridge) pig.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
            this.bridge$pushTransformReason(EntityTransformEvent.TransformReason.LIGHTNING);
            converted = (Mob) DecorationOps.callsite().invoke(pig, entityType, conversionParams, afterConversion);
            cancelled = this.felt$pigZapCancelled;
        } finally {
            this.felt$lightningBolt = null;
            this.felt$pigZapCancelled = false;
        }
        if (cancelled) {
            return (Mob) DecorationOps.cancel().invoke();
        }
        return converted;
    }

    private transient LightningBolt felt$lightningBolt;
    private transient boolean felt$pigZapCancelled;

    @Override
    public void bridge$cancelPigZap() {
        this.felt$pigZapCancelled = true;
    }

    @Override
    public LightningBolt bridge$getPigZapLightning() {
        return this.felt$lightningBolt;
    }

    @Override
    public void bridge$clearPigZapLightning() {
        this.felt$lightningBolt = null;
    }
}
