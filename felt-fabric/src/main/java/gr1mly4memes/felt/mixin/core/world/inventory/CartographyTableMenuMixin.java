package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.world.inventory.PosContainerBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import org.bukkit.craftbukkit.inventory.CraftInventoryCartography;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenuMixin implements PosContainerBridge {

    // @formatter:off
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final public Container container;
    @Shadow @Final private ResultContainer resultContainer;
    // @formatter:on

    private CraftInventoryView<CartographyTableMenu, ?> view = null; // Paper added
    private Player player;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void felt$init(int id, Inventory playerInventory, ContainerLevelAccess worldPosCallable, CallbackInfo ci) {
        this.player = ((ServerPlayerBridge) playerInventory.player).bridge$getBukkitEntity();
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void felt$unreachable(net.minecraft.world.entity.player.Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Decorate(method = "slotsChanged(Lnet/minecraft/world/Container;)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void felt$preserveEditedResult(Container changed) throws Throwable {
        // Result-slot writes must not recompute the output edited by an inventory click listener.
        // Keep the input-container callback and the native result calculation unchanged.
        if (changed == this.resultContainer) {
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Override
    public CraftInventoryView<CartographyTableMenu, ?> getBukkitView() {
        if (this.view != null) {
            return this.view;
        }

        CraftInventoryCartography inventory = new CraftInventoryCartography(this.container, this.resultContainer);
        this.view = new CraftInventoryView<>(this.player, inventory, (CartographyTableMenu) (Object) this);
        return this.view;
    }

    @Override
    public ContainerLevelAccess bridge$getWorldPos() {
        return this.access;
    }
}
