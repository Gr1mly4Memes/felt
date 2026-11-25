package gr1mly4memes.felt.mixin.core.world.entity.npc;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gr1mly4memes.felt.mod.server.world.FeltExperience;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import org.bukkit.entity.ExperienceOrb.SpawnReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Villager.class, WanderingTrader.class})
public abstract class VillagerExperienceMixin {
    @ModifyExpressionValue(method = "rewardTradeXp", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDDI)Lnet/minecraft/world/entity/ExperienceOrb;"))
    private ExperienceOrb felt$attributeTradeExperience(ExperienceOrb orb) {
        var villager = (AbstractVillager) (Object) this;
        return FeltExperience.withAttribution(orb, SpawnReason.VILLAGER_TRADE, villager.getTradingPlayer(), villager);
    }
}
