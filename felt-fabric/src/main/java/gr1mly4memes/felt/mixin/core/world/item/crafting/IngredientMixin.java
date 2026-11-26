package gr1mly4memes.felt.mixin.core.world.item.crafting;

import gr1mly4memes.felt.bridge.core.world.item.crafting.IngredientBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Ingredient.class)
public abstract class IngredientMixin implements IngredientBridge {

    private List<ItemStack> exactChoices;
    private java.util.function.Predicate<ItemStack> stackPredicate;

    @Override
    public void bridge$setStackPredicate(java.util.function.Predicate<ItemStack> predicate) {
        this.stackPredicate = predicate;
    }

    @Override
    public java.util.function.Predicate<ItemStack> bridge$getStackPredicate() {
        return this.stackPredicate;
    }

    @gr1mly4memes.felt.mod.mixins.annotation.TransformAccess(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC)
    private static Ingredient ofStacks(List<ItemStack> stacks) {
        Ingredient ingredient = Ingredient.of(stacks.stream().map(ItemStack::getItem));
        ((IngredientBridge) (Object) ingredient).bridge$setExactChoices(stacks);
        return ingredient;
    }

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", cancellable = true, at = @At("HEAD"))
    private void felt$exactMatch(@Nullable ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (this.stackPredicate != null) {
            cir.setReturnValue(this.stackPredicate.test(itemStack));
            return;
        }
        if (this.exactChoices != null) {
            if (itemStack == null) {
                cir.setReturnValue(false);
                return;
            }
            for (ItemStack exactChoice : this.exactChoices) {
                if (ItemStack.isSameItemSameComponents(itemStack, exactChoice)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
            cir.setReturnValue(false);
        }
    }

    @Override
    public void bridge$setExactChoices(List<ItemStack> exactChoices) {
        this.exactChoices = exactChoices.stream().map(ItemStack::copy).toList();
    }

    @Inject(method = "display", at = @At("HEAD"), cancellable = true)
    private void felt$displayExactChoices(CallbackInfoReturnable<SlotDisplay> cir) {
        if (this.exactChoices != null) {
            cir.setReturnValue(new SlotDisplay.Composite(this.exactChoices.stream().filter(stack -> !stack.isEmpty())
                .map(ItemStackTemplate::fromNonEmptyStack).<SlotDisplay>map(SlotDisplay.ItemStackSlotDisplay::new).toList()));
        }
    }

    @Override
    public List<ItemStack> bridge$getExactChoices() {
        if (this.exactChoices == null) {
            return null;
        }
        return this.exactChoices.stream().map(ItemStack::copy).toList();
    }

    @Override
    public boolean bridge$isExact() {
        return this.exactChoices != null;
    }
}
