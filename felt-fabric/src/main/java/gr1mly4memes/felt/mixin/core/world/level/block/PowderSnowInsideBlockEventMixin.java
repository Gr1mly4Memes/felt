package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import gr1mly4memes.felt.mixin.core.world.entity.EntityInsideBlockFrozenDataAccessor;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowInsideBlockEventMixin {

    @Decorate(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V",
        inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void felt$powderSnowInsideBlock(BlockState state, Level level, BlockPos pos, Entity entity,
                                              InsideBlockEffectApplier effectApplier, boolean isPrecise) throws Throwable {
        if (!level.isClientSide()
            && !new EntityInsideBlockEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(level, pos)).callEvent()) {
            if (entity instanceof ServerPlayer player) {
                var bukkitPlayer = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
                if (bukkitPlayer.canSee(bukkitPlayer)) {
                    // Paper's targeted resend: include the current value even if
                    // default, without draining dirty or unrelated metadata.
                    var frozenTicks = EntityInsideBlockFrozenDataAccessor.felt$insideBlockFrozenTicks();
                    player.connection.send(new ClientboundSetEntityDataPacket(player.getId(),
                        List.of(SynchedEntityData.DataValue.create(frozenTicks, player.getEntityData().get(frozenTicks)))));
                }
            }
            // No stuck multiplier, snow effects, melt callback or freeze/extinguish
            // work from this block may be queued after the event veto.
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
