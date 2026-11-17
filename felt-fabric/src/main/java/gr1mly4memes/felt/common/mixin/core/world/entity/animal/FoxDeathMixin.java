package gr1mly4memes.felt.common.mixin.core.world.entity.animal;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.common.bridge.core.world.entity.DeathEventBridge;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@gr1mly4memes.felt.common.mod.mixins.annotation.OnlyInPlatform({gr1mly4memes.felttools.api.FeltPlatform.VANILLA, gr1mly4memes.felttools.api.FeltPlatform.FABRIC})
@Mixin(Fox.class)
public class FoxDeathMixin {
    @WrapOperation(method = {"dropAllDeathLoot", "dropEquipment"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/fox/Fox;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$deferMouth(Fox fox, EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        ((DeathEventBridge) fox).bridge$deferDeathChange(() -> original.call(fox, slot, stack));
    }
}
