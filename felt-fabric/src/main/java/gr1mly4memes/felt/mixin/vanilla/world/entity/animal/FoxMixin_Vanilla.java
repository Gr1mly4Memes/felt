package gr1mly4memes.felt.mixin.vanilla.world.entity.animal;

import gr1mly4memes.felt.mixin.core.world.entity.animal.AnimalMixin;
import gr1mly4memes.felt.mod.util.FeltCaptures;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Fox.class)
public abstract class FoxMixin_Vanilla extends AnimalMixin {

    @Decorate(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/fox/Fox;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity felt$captureFoxDrop(Fox instance, ServerLevel level, ItemStack itemStack) throws Throwable {
        try {
            felt$spawnNoAdd = true;
            final var result = (ItemEntity) DecorationOps.callsite().invoke(instance, level, itemStack);
            FeltCaptures.captureExtraDrops(List.of(result));
            return result;
        } finally {
            felt$spawnNoAdd = false;
        }
    }
}
