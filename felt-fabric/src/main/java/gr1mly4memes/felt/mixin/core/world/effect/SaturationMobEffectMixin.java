package gr1mly4memes.felt.mixin.core.world.effect;

import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.effect.SaturationMobEffect")
public class SaturationMobEffectMixin {

    @Decorate(method = "applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void felt$foodLevelChange(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, ServerLevel serverLevel, LivingEntity livingEntity, int amplifier) throws Throwable {
        Player playerEntity = ((Player) livingEntity);
        int oldFoodLevel = playerEntity.getFoodData().getFoodLevel();
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(playerEntity, foodLevelIn + oldFoodLevel);
        if (!event.isCancelled()) {
            DecorationOps.callsite().invoke(foodStats, event.getFoodLevel() - oldFoodLevel, foodSaturationModifier);
        }
        ((ServerPlayer) playerEntity).connection.send(new ClientboundSetHealthPacket(((ServerPlayerBridge) playerEntity).bridge$getBukkitEntity().getScaledHealth(),
            playerEntity.getFoodData().getFoodLevel(), playerEntity.getFoodData().getSaturationLevel()));
    }
}
