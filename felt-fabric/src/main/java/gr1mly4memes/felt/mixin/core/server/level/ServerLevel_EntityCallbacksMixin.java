package gr1mly4memes.felt.mixin.core.server.level;

import com.google.common.collect.Lists;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.bridge.core.world.level.saveddata.maps.MapItemSavedDataBridge;
import gr1mly4memes.felt.mod.server.FeltServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/server/level/ServerLevel$EntityCallbacks")
public class ServerLevel_EntityCallbacksMixin {
    @Shadow(aliases = {"f_143351_", "this$0", "field_26936"})
    private ServerLevel outerThis;

    @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void felt$valid(Entity entity, CallbackInfo ci) {
        ((EntityBridge) entity).bridge$setInWorld(true);
        ((EntityBridge) entity).bridge$setValid(true);
        ((gr1mly4memes.felt.bridge.core.entity.PaperEntityStateBridge)entity).bridge$captureOrigin();
        new com.destroystokyo.paper.event.entity.EntityAddToWorldEvent(((EntityBridge) entity).bridge$getBukkitEntity(),
            ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge) this.outerThis).bridge$getWorld()).callEvent();
    }

    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void felt$entityCleanup(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player) {
            SavedDataStorage worldData = FeltServer.getMinecraftServer().getDataStorage();
            for (var savedData : worldData.cache.values()) {
                if (savedData.orElse(null) instanceof MapItemSavedData map) {
                    map.carriedByPlayers.remove(player);
                    ((MapItemSavedDataBridge) map).bridge$getCarriedBy().removeIf(holdingPlayer -> holdingPlayer.player == entity);
                }
            }
        }
        if (((EntityBridge) entity).bridge$getBukkitEntity() instanceof InventoryHolder holder) {
            for (org.bukkit.entity.HumanEntity h : Lists.newArrayList(holder.getInventory().getViewers())) {
                h.closeInventory();
            }
        }
    }

    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void felt$invalid(Entity entity, CallbackInfo ci) {
        ((EntityBridge) entity).bridge$setValid(false);
        if (!(entity instanceof ServerPlayer)) {
            for (var player : outerThis.getServer().getPlayerList().getPlayers()) {
                ((ServerPlayerBridge) player).bridge$getBukkitEntity().onEntityRemove(entity);
            }
        }
        new com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent(((EntityBridge) entity).bridge$getBukkitEntity(),
            ((gr1mly4memes.felt.bridge.core.world.level.WorldBridge) this.outerThis).bridge$getWorld()).callEvent();
    }
}
