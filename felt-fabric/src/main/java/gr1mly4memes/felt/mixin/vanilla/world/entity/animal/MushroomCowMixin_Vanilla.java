package gr1mly4memes.felt.mixin.vanilla.world.entity.animal;

import gr1mly4memes.felt.bridge.core.world.entity.MobBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.mixin.vanilla.world.entity.EntityMixin_Vanilla;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.item.ItemStack;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin_Vanilla extends EntityMixin_Vanilla {

    @Decorate(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob felt$animalTransform(MushroomCow mushroomCow, EntityType<? extends Mob> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion afterConversion, ServerLevel level, SoundSource source, ItemStack stack) throws Throwable {
        ((WorldBridge) mushroomCow.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SHEARED);
        ((MobBridge) mushroomCow).bridge$pushTransformReason(EntityTransformEvent.TransformReason.SHEARED);
        return (Mob) DecorationOps.callsite().invoke(mushroomCow, entityType, conversionParams, afterConversion);
    }
}
