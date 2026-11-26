package gr1mly4memes.felt.mixin.core.world.entity.ai.behavior;

import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {

    @Decorate(method = "isTargetValidToPick(Lnet/minecraft/world/entity/PathfinderMob;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/util/Set;Ljava/util/Set;Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers$TransportItemTarget;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers;isContainerLocked(Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers$TransportItemTarget;)Z"), require = 1, allow = 1)
    private boolean felt$validateTransportTarget(TransportItemsBetweenContainers behavior, TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob body, Level level) throws Throwable {
        boolean locked = (boolean) DecorationOps.callsite().invoke(behavior, target);
        // Reuse the native rejection branch without stopping the remaining container search.
        return locked || !CraftEventFactory.callTransporterValidateTarget(body, level, target.pos());
    }

    @Inject(method = "matchesLeavingItemsRequirement", at = @At("HEAD"), cancellable = true)
    private static void felt$decideSortDestination(PathfinderMob mob, Container container, CallbackInfoReturnable<Boolean> cir) {
        var event = CraftEventFactory.createItemTransportingEntitySortEvent(mob, container);
        event.callEvent();
        if (event.getResult() != Event.Result.DEFAULT) {
            cir.setReturnValue(event.getResult() == Event.Result.ALLOW);
        }
    }

    @Inject(method = "targetHasNotChanged", at = @At("RETURN"), cancellable = true)
    private void felt$rejectRemovedDestination(Level level, TransportItemsBetweenContainers.TransportItemTarget target,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && felt$isContainerRemoved(target.container())) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean felt$isContainerRemoved(Container container) {
        if (container instanceof CompoundContainer combined) {
            return felt$isContainerRemoved(combined.container1) || felt$isContainerRemoved(combined.container2);
        }
        return container instanceof BlockEntity entity && entity.isRemoved();
    }
}
