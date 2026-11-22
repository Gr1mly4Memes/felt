package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HopperMenu;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperMenu.class)
public abstract class HopperMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private Container hopper;
    // @formatter:on

    private CraftInventoryView<HopperMenu, ?> view = null; // Paper added
    private Inventory inventory; // Paper added

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V", at = @At("RETURN"))
    public void felt$init(int id, Inventory playerInventory, Container inventory, CallbackInfo ci) {
        this.inventory = playerInventory;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void felt$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftInventoryView<HopperMenu, ?> getBukkitView() {
        if (this.view != null) {
            return this.view;
        }
        CraftInventory inventory = new CraftInventory(this.hopper);
        this.view = new CraftInventoryView<>(((PlayerBridge) this.inventory.player).bridge$getBukkitEntity(), inventory, (HopperMenu) (Object) this);
        return this.view;
    }
}
