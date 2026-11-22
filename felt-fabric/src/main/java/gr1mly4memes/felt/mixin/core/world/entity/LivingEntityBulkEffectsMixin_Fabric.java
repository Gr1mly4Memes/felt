package gr1mly4memes.felt.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import gr1mly4memes.felt.mod.util.BulkClearMapView;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Composes with Fabric entity-events' Map.clear wrapper. Bukkit decides against
 * an immutable source snapshot; the existing Fabric operation still owns its
 * clear/restore veto chain.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBulkEffectsMixin_Fabric {

    @WrapOperation(method = "removeAllEffects", at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"))
    private void felt$clearOnlyBukkitAcceptedEffects(Map<Holder<MobEffect>, MobEffectInstance> activeEffects,
                                                           Operation<Void> original) {
        Set<Map.Entry<Holder<MobEffect>, MobEffectInstance>> snapshot =
            BulkClearMapView.immutableEntrySnapshot(activeEffects.entrySet());
        Map<Holder<MobEffect>, MobEffectInstance> accepted = new HashMap<>();
        for (Map.Entry<Holder<MobEffect>, MobEffectInstance> entry : snapshot) {
            if (((LivingEntityBridge) this).bridge$willRemoveEffect(entry.getValue())) {
                accepted.put(entry.getKey(), entry.getValue());
            }
        }
        // Preserve the complete original operation chain, including Fabric's hook.
        original.call(new BulkClearMapView<>(activeEffects, accepted));
    }

}
