package gr1mly4memes.felt.mixin.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import gr1mly4memes.felt.bridge.core.world.inventory.PrepareResultMenuBridge;
import gr1mly4memes.felt.bridge.core.world.inventory.ContainerLevelAccessBridge;
import gr1mly4memes.felt.mod.server.world.inventory.FeltSmithingView;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventorySmithing;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenuMixin {

    private CraftInventoryView<SmithingMenu, ?> bukkitEntity;

    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void felt$prepareSmithing(ResultContainer craftResultInventory, int index, ItemStack stack) throws Throwable {
        final CraftInventoryView<SmithingMenu, ?> craft = getBukkitView();
        if (craft instanceof FeltSmithingView) {
            // Call preparing event; preserve injection logic
            if (!((PrepareResultMenuBridge) this).bridge$isPrepareResultDeferred()) {
                PrepareSmithingEvent event = new PrepareSmithingEvent(craft, CraftItemStack.asCraftMirror(stack).clone());
                Bukkit.getServer().getPluginManager().callEvent(event);
                stack = CraftItemStack.asNMSCopy(event.getResult());
            }
            DecorationOps.callsite().invoke(craftResultInventory, index, stack);
        } else {
            // Run plugin custom logic
            CraftEventFactory.callPrepareSmithingEvent(craft, stack);
            if (!((PrepareResultMenuBridge) this).bridge$isPrepareResultDeferred()) {
                CraftEventFactory.callPrepareResultEvent((SmithingMenu) (Object) this, SmithingMenu.RESULT_SLOT);
            }
        }
    }

    @Override
    public CraftInventoryView<SmithingMenu, ?> getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }
        CraftInventorySmithing inventory = new CraftInventorySmithing(((ContainerLevelAccessBridge) this.access).bridge$getLocation(), this.inputSlots, this.resultSlots);
        return this.bukkitEntity = new FeltSmithingView(((PlayerBridge) this.player).bridge$getBukkitEntity(), inventory, (SmithingMenu) (Object) this);
    }
}
