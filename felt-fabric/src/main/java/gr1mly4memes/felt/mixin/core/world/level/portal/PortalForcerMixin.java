package gr1mly4memes.felt.mixin.core.world.level.portal;

import gr1mly4memes.felt.bridge.core.world.level.portal.PortalForcerBridge;
import net.minecraft.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalForcer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.BlockStateListPopulator;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(value = PortalForcer.class, priority = 1500)
public abstract class PortalForcerMixin implements PortalForcerBridge {

    // @formatter:off
    @Shadow public abstract Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis);
    @Shadow @Final protected ServerLevel level;
    // @formatter:on

    private transient int felt$searchRadius = -1;

    @Override
    public void bridge$pushSearchRadius(int searchRadius) {
        this.felt$searchRadius = searchRadius;
    }

    @ModifyVariable(method = "findClosestPortalPosition", ordinal = 0, at = @At(value = "STORE", ordinal = 0))
    private int felt$useSearchRadius(int i) {
        return this.felt$searchRadius == -1 ? i : this.felt$searchRadius;
    }

    @ModifyArg(method = "createPortal", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;spiralAround(Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Ljava/lang/Iterable;"))
    private int felt$changeRadius(int i) {
        return this.felt$createRadius == -1 ? i : this.felt$createRadius;
    }

    @Redirect(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean felt$captureBlocks1(ServerLevel serverWorld, BlockPos pos, BlockState state) {
        if (this.felt$populator == null) {
            this.felt$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.felt$populator.setBlock(pos, state, 3);
    }

    @Redirect(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean felt$captureBlocks2(ServerLevel serverWorld, BlockPos pos, BlockState state, int flags) {
        if (this.felt$populator == null) {
            this.felt$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.felt$populator.setBlock(pos, state, flags);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "createPortal", cancellable = true, at = @At("RETURN"))
    private void felt$portalCreate(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        CraftWorld craftWorld = this.level.bridge$getWorld();
        List<org.bukkit.block.BlockState> blockStates;
        if (this.felt$populator == null) {
            blockStates = new ArrayList<>();
        } else {
            blockStates = (List) this.felt$populator.getSnapshotBlocks();
        }
        PortalCreateEvent event = new PortalCreateEvent(blockStates, craftWorld, (this.felt$entity == null) ? null : this.felt$entity.bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.NETHER_PAIR);

        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        if (this.felt$populator != null) {
            this.felt$populator.placeBlocks();
        }
    }

    private transient BlockStateListPopulator felt$populator;
    private transient Entity felt$entity;
    private transient int felt$createRadius = -1;

    @Override
    public void bridge$pushPortalCreate(Entity entity, int createRadius) {
        this.felt$entity = entity;
        this.felt$createRadius = createRadius;
    }
}
