package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import io.papermc.paper.event.block.BlockBreakProgressUpdateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelBlockBreakProgressEventMixin {

    @Shadow public abstract Entity getEntity(int id);

    @Decorate(method = "destroyBlockProgress(ILnet/minecraft/core/BlockPos;I)V", inject = true,
        at = @At("HEAD"), require = 1, allow = 1)
    private void felt$blockBreakProgressEvent(int id, BlockPos pos, int progress) {
        Entity entity = this.getEntity(id);
        // Client-only entity ids do not represent server-side breaking progress.
        if (entity != null) {
            new BlockBreakProgressUpdateEvent(CraftBlock.at((ServerLevel) (Object) this, pos),
                Mth.clamp(progress, 0, 10) / 10.0F, ((EntityBridge) entity).bridge$getBukkitEntity()).callEvent();
        }
    }
}
