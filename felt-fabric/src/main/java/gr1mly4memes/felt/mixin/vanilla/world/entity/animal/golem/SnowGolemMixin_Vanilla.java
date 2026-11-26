package gr1mly4memes.felt.mixin.vanilla.world.entity.animal.golem;

import gr1mly4memes.felt.mixin.core.world.entity.PathfinderMobMixin;
import gr1mly4memes.felt.mod.server.event.FeltShearing;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin_Vanilla extends PathfinderMobMixin {

    // ShearsItem#interactLivingEntity
    @Decorate(method = "mobInteract", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/SnowGolem;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void felt$onShear(SnowGolem snowGolem, ServerLevel level, SoundSource soundSource, ItemStack stack, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaultDrops = FeltShearing.defaultDrops(snowGolem, level, stack);
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, snowGolem, stack, hand, defaultDrops);
        if (event != null && event.isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }

        List<ItemStack> drops = event == null ? defaultDrops : CraftItemStack.asNMSCopy(event.getDrops());
        var previous = FeltShearing.captureDrops(snowGolem, stack, drops);
        try {
            DecorationOps.callsite().invoke(snowGolem, level, soundSource, stack);
        } finally {
            FeltShearing.restoreDrops(previous);
        }
    }
}
