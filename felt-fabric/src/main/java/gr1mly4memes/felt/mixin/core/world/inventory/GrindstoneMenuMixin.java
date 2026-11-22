package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import gr1mly4memes.felt.bridge.core.world.inventory.PosContainerBridge;
import gr1mly4memes.felt.mod.server.world.inventory.FeltGrindstoneView;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryGrindstone;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenuMixin implements PosContainerBridge {

    @Shadow @Final Container repairSlots;
    @Shadow @Final private Container resultSlots;
    @Shadow @Final private ContainerLevelAccess access;
    private CraftInventoryView<GrindstoneMenu, ?> view = null; // Paper added
    private Inventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void felt$init(int windowIdIn, Inventory playerInventory, ContainerLevelAccess worldPosCallableIn, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Decorate(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void felt$prepareEvent(Container instance, int i, ItemStack itemStack) throws Throwable {
        final CraftInventoryView<GrindstoneMenu, ?> craft = getBukkitView();
        if (craft instanceof FeltGrindstoneView) {
            // Call prepare event; preserve injection point
            PrepareGrindstoneEvent event = new PrepareGrindstoneEvent(craft, CraftItemStack.asCraftMirror(itemStack).clone());
            event.getView().getPlayer().getServer().getPluginManager().callEvent(event);
            DecorationOps.callsite().invoke(instance, i, CraftItemStack.asNMSCopy(event.getResult()));
        } else {
            // Run plugin custom logic
            CraftEventFactory.callPrepareGrindstoneEvent(getBukkitView(), itemStack);
            CraftEventFactory.callPrepareResultEvent((GrindstoneMenu) (Object) this, GrindstoneMenu.RESULT_SLOT);
        }
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/GrindstoneMenu;broadcastChanges()V"))
    private void felt$sync(CallbackInfo ci) {
        sendAllDataToRemote();
    }

    @Override
    public CraftInventoryView<GrindstoneMenu, ?> getBukkitView() {
        if (this.view != null) {
            return this.view;
        }

        CraftInventoryGrindstone inventory = new CraftInventoryGrindstone(this.repairSlots, this.resultSlots);
        this.view = new FeltGrindstoneView(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (GrindstoneMenu) (Object) this);
        return this.view;
    }

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void felt$unreachable(net.minecraft.world.entity.player.Player player,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public ContainerLevelAccess bridge$getWorldPos() {
        return this.access;
    }
}
