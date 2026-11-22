package gr1mly4memes.felt.mixin.core.world.entity;

import gr1mly4memes.felt.mod.util.VillagerZapConversion;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class MobVillagerZapEventMixin {

    @Decorate(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ConversionParams$AfterConversion;finalizeConversion(Lnet/minecraft/world/entity/Mob;)V"))
    private void felt$villagerZapBeforeFinalize(ConversionParams.AfterConversion afterConversion,
            Mob replacement) throws Throwable {
        if (afterConversion instanceof VillagerZapConversion<?> zap && !zap.callEvent(replacement)) {
            // A native null conversion returns to Villager.thunderHit's retained fallback.
            // Do not finalize, release POIs, send Neo's post-hook, admit or discard.
            DecorationOps.cancel().invoke((Mob) null);
            return;
        }
        DecorationOps.callsite().invoke(afterConversion, replacement);
    }
}
