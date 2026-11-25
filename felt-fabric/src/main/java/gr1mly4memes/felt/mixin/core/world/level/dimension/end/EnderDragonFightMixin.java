package gr1mly4memes.felt.mixin.core.world.level.dimension.end;

import gr1mly4memes.felt.bridge.core.world.level.dimension.end.EnderDragonFightBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import io.papermc.paper.event.block.DragonEggFormEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.saveddata.SavedData;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.boss.CraftDragonBattle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Preserve the native void respawn paths and derive Bukkit's result from the transition. */
@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin extends SavedData implements EnderDragonFightBridge {

    @Shadow public DragonRespawnStage respawnStage;
    @Shadow private boolean dragonKilled;
    @Shadow public boolean hasPreviouslyKilledDragon;
    @Shadow public abstract void tryRespawn();
    @Unique private boolean felt$emptyCrystalRespawnAllowed;

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
        target = "Ljava/util/List;isEmpty()Z"), require = 1, allow = 1)
    private boolean felt$requireRespawnCrystals(boolean empty) {
        return empty && !this.felt$emptyCrystalRespawnAllowed;
    }

    @Inject(method = "abortRespawnSequence", at = @At("HEAD"))
    private void felt$resetCrystalExemptionOnAbort(CallbackInfo ci) {
        this.felt$emptyCrystalRespawnAllowed = false;
    }

    @Inject(method = "setRespawnStage", at = @At("RETURN"))
    private void felt$resetCrystalExemptionOnCompletion(DragonRespawnStage stage, CallbackInfo ci) {
        if (stage == DragonRespawnStage.END) {
            this.felt$emptyCrystalRespawnAllowed = false;
        }
    }

    @Invoker("respawnDragon")
    public abstract void felt$invokeRespawnDragon(List<EndCrystal> crystals);

    @Override
    public boolean bridge$tryRespawn() {
        // Keep the native portal/crystal search; do not count an already-active fight as success.
        boolean eligible = this.dragonKilled && this.respawnStage == null;
        this.tryRespawn();
        return eligible && this.respawnStage == DragonRespawnStage.START;
    }

    @Override
    public boolean bridge$respawnDragon(List<EndCrystal> crystals) {
        boolean eligible = this.dragonKilled && this.respawnStage == null;
        this.felt$invokeRespawnDragon(crystals);
        boolean started = eligible && this.respawnStage == DragonRespawnStage.START;
        if (started) {
            this.felt$emptyCrystalRespawnAllowed = crystals.isEmpty();
        }
        return started;
    }

    @Decorate(method = "setDragonKilled(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;)V",
        at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;hasPreviouslyKilledDragon:Z"),
        require = 1, allow = 1)
    private boolean felt$includeDragonEggAttempt(EnderDragonFight fight,
            @Local(allocate = "feltPreviouslyKilledDragon") boolean previouslyKilled) throws Throwable {
        previouslyKilled = (boolean) DecorationOps.callsite().invoke(fight);
        DecorationOps.blackhole().invoke(previouslyKilled);
        // Paper dispatches on every tracked dragon death, including the normally cancelled repeat attempt.
        // Change only this branch operand, not the fight's persisted previous-kill state.
        return false;
    }

    @Decorate(method = "setDragonKilled(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
        require = 1, allow = 1)
    private boolean felt$dragonEggForm(ServerLevel level, BlockPos pos, BlockState proposed,
            @Local(allocate = "feltPreviouslyKilledDragon") boolean previouslyKilled) throws Throwable {
        CraftBlockState state = CraftBlockStates.getBlockState(level, pos);
        state.setBlock(proposed);
        DragonEggFormEvent event = new DragonEggFormEvent(CraftBlock.at(level, pos), state,
            new CraftDragonBattle((EnderDragonFight) (Object) this));
        event.setCancelled(previouslyKilled && !((gr1mly4memes.felt.bridge.core.world.level.WorldBridge) level)
            .bridge$paperConfig().entities.behavior.enderDragonsDeathAlwaysPlacesDragonEgg);
        if (!event.callEvent()) {
            return false;
        }
        // Native setBlockAndUpdate uses UPDATE_ALL, as Paper's CraftBlockState.place does.
        return (boolean) DecorationOps.callsite().invoke(level, pos, ((CraftBlockState) event.getNewState()).getHandle());
    }

    @Override
    public void bridge$setPreviouslyKilled(boolean killed) {
        if (this.hasPreviouslyKilledDragon != killed) {
            this.hasPreviouslyKilledDragon = killed;
            this.setDirty();
        }
    }
}
