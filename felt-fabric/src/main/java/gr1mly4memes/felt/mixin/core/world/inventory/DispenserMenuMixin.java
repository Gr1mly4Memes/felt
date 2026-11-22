package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DispenserMenu;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserMenu.class)
public abstract class DispenserMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final public Container dispenser;
    // @formatter:on

    private CraftInventoryView<DispenserMenu, ?> view; // Paper added
    private Inventory inventory; // Paper added

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V", at = @At("RETURN"))
    public void felt$init(int p_i50088_1_, Inventory playerInventory, Container p_i50088_3_, CallbackInfo ci) {
        this.inventory = playerInventory;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void felt$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftInventoryView<DispenserMenu, ?> getBukkitView() {
        if (this.view != null) {
            return this.view;
        }

        CraftInventory inventory = new CraftInventory(this.dispenser);
        this.view = new CraftInventoryView<>(((PlayerBridge) this.inventory.player).bridge$getBukkitEntity(), inventory, (DispenserMenu) (Object) this);
        return this.view;
    }
}
