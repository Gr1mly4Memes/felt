package gr1mly4memes.felt.mixin.core.world.item;

import gr1mly4memes.felt.bridge.core.world.item.ItemCooldownsBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.player.PlayerItemGroupCooldownEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin implements ItemCooldownsBridge {

    @Shadow @Final private Map<Identifier, ItemCooldowns.CooldownInstance> cooldowns;
    @Shadow private int tickCount;

    @Unique private ItemStack felt$cooldownItem;

    @Decorate(method = "addCooldown(Lnet/minecraft/world/item/ItemStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/resources/Identifier;I)V"))
    private void felt$itemCooldown(ItemCooldowns cooldowns, Identifier group, int ticks, ItemStack item, int duration) throws Throwable {
        ItemStack previous = this.felt$cooldownItem;
        this.felt$cooldownItem = item;
        try {
            DecorationOps.callsite().invoke(cooldowns, group, ticks);
        } finally {
            this.felt$cooldownItem = previous;
        }
    }

    @Decorate(method = "addCooldown(Lnet/minecraft/resources/Identifier;I)V", inject = true, at = @At("HEAD"))
    private void felt$cooldownEvent(Identifier group, int ticks) throws Throwable {
        // Claim the item before dispatch so listener-initiated group calls remain group events.
        ItemStack item = this.felt$cooldownItem;
        this.felt$cooldownItem = null;
        PlayerItemGroupCooldownEvent event = this.felt$createCooldownEvent(item, group, ticks);
        if (event != null) {
            if (!event.callEvent()) {
                this.felt$resyncCooldown(group);
                DecorationOps.cancel().invoke();
                return;
            }
            ticks = event.getCooldown();
            DecorationOps.blackhole().invoke(ticks);
        }
    }

    protected PlayerItemGroupCooldownEvent felt$createCooldownEvent(ItemStack item, Identifier group, int ticks) {
        return null;
    }

    protected void felt$resyncCooldown(Identifier group) {
    }

    @Override
    public int bridge$getRemainingCooldown(Identifier cooldownGroup) {
        return this.felt$currentCooldown(cooldownGroup);
    }

    protected int felt$currentCooldown(Identifier group) {
        ItemCooldowns.CooldownInstance cooldown = this.cooldowns.get(group);
        return cooldown == null ? 0 : Math.max(0, cooldown.endTime() - this.tickCount);
    }
}
