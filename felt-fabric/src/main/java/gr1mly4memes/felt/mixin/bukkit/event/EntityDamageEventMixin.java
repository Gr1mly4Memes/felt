package gr1mly4memes.felt.mixin.bukkit.event;

import com.google.common.base.Function;
import gr1mly4memes.felt.bridge.bukkit.EntityDamageEventBridge;
import net.minecraft.util.Mth;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.Map;

@Mixin(value = EntityDamageEvent.class, remap = false)
public abstract class EntityDamageEventMixin extends Event implements EntityDamageEventBridge {

    @Shadow @Final private Map<EntityDamageEvent.DamageModifier, Double> modifiers;

    @Unique
    private Map<EntityDamageEvent.DamageModifier, ? extends Function<? super Double, Double>> felt$originalFunction;

    @Inject(method = "<init>(Lorg/bukkit/entity/Entity;Lorg/bukkit/event/entity/EntityDamageEvent$DamageCause;Lorg/bukkit/damage/DamageSource;Ljava/util/Map;Ljava/util/Map;)V", at = @At("RETURN"))
    private void felt$init(Entity damagee, EntityDamageEvent.DamageCause cause, DamageSource source, Map modifiers, Map modifierFunctions, CallbackInfo ci) {
        felt$originalFunction = new EnumMap<>(EntityDamageEvent.DamageModifier.class);
        felt$originalFunction.putAll(modifierFunctions);
    }

    @Override
    public boolean felt$applicable(EntityDamageEvent.DamageModifier stage) {
        return modifiers.containsKey(stage);
    }

    @Override
    public double felt$applyOriginal(EntityDamageEvent.DamageModifier currentStage, double lastStage) {
        return felt$originalFunction.get(currentStage).apply(lastStage);
    }

    @Override
    public boolean felt$isStillOriginal(EntityDamageEvent.DamageModifier modifier, double last, double current) {
        return Math.abs((current - last) - felt$applyOriginal(modifier, last)) < Mth.EPSILON;
    }
}
