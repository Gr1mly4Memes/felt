package gr1mly4memes.felt.mixin.core.util.worldupdate;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gr1mly4memes.felt.bridge.core.world.level.storage.LevelStorageSourceBridge.LevelStorageAccessBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.util.worldupdate.WorldUpgrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.Set;

@Mixin(WorldUpgrader.class)
public abstract class WorldUpgraderMixin {

    @ModifyExpressionValue(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;registryKeySet()Ljava/util/Set;"),
        require = 1, expect = 1, allow = 1
    )
    private Set<ResourceKey<LevelStem>> felt$restrictTypedDimensionEnumeration(
        Set<ResourceKey<LevelStem>> registryDimensions,
        @Local(argsOnly = true) LevelStorageSource.LevelStorageAccess levelSource
    ) {
        final ResourceKey<LevelStem> typeKey = ((LevelStorageAccessBridge) (Object) levelSource).bridge$getTypeKey();
        return typeKey == null ? registryDimensions : Collections.singleton(typeKey);
    }
}
