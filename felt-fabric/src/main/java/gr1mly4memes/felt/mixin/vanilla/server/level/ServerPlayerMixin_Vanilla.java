package gr1mly4memes.felt.mixin.vanilla.server.level;

import com.mojang.datafixers.util.Either;
import gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Vanilla extends Player implements ServerPlayerBridge {

    protected ServerPlayerMixin_Vanilla(net.minecraft.world.level.Level level, com.mojang.authlib.GameProfile profile) {
        super(level, profile);
    }


    @Inject(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V"))
    private void felt$bedCause(net.minecraft.world.level.block.AbstractBedBlock bedBlock, net.minecraft.world.level.block.state.BlockState bedBlockState, net.minecraft.world.attribute.BedRule rule, BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.BED);
    }

    @Inject(method = "startSleepInBed", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startSleepInBed(Lnet/minecraft/world/level/block/AbstractBedBlock;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/attribute/BedRule;Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;"))
    private void felt$beforeActualSleep(net.minecraft.world.level.block.AbstractBedBlock bedBlock, net.minecraft.world.level.block.state.BlockState bedState, net.minecraft.world.attribute.BedRule rule, BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        Either<Player.BedSleepingProblem, Unit> decision = bridge$fireBedEvent(Either.right(Unit.INSTANCE), pos);
        if (decision.left().isPresent()) {
            cir.setReturnValue(decision);
        } else {
            bridge$markBedEnterEventFired();
        }
    }

    @Redirect(method = "startSleepInBed", require = 0, at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;left(Ljava/lang/Object;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> felt$failSleep(L value, net.minecraft.world.level.block.AbstractBedBlock bedBlock, net.minecraft.world.level.block.state.BlockState bedState, net.minecraft.world.attribute.BedRule rule, BlockPos pos) {
        boolean canSetSpawn = rule.canSetSpawn(this.level());
        Either<L, R> decision = bridge$fireBedEvent(Either.left(value), pos);
        if (decision.left().isPresent()) return decision;
        ServerPlayer player = (ServerPlayer) (Object) this;
        Either<Player.BedSleepingProblem, Unit> slept = felt$invokeParentSleep(bedBlock, bedState, rule, pos);
        slept.ifRight(unit -> {
            player.awardStat(bedBlock.getSleptInBedStatType());
            if (canSetSpawn) {
                net.minecraft.advancements.triggers.CriteriaTriggers.SLEPT_IN_BED.trigger(player);
            }
        });
        if (!player.level().canSleepThroughNights()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("sleep.not_possible"));
        }
        player.level().updateSleepingPlayerList();
        return (Either<L, R>) slept;
    }
    private Either<Player.BedSleepingProblem, Unit> felt$invokeParentSleep(net.minecraft.world.level.block.AbstractBedBlock bedBlock, net.minecraft.world.level.block.state.BlockState bedState, net.minecraft.world.attribute.BedRule rule, BlockPos pos) {
        return super.startSleepInBed(bedBlock, bedState, rule, pos);
    }
    @gr1mly4memes.felttools.mixin.Decorate(method = "drop(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private net.minecraft.world.entity.item.ItemEntity felt$recordSelectedDrop(ServerPlayer player, net.minecraft.world.item.ItemStack stack, boolean thrownFromHand, net.minecraft.util.Prediction prediction) throws Throwable {
        var dropped = (net.minecraft.world.entity.item.ItemEntity) gr1mly4memes.felttools.mixin.DecorationOps.callsite().invoke(player, stack, thrownFromHand, prediction);
        ((ServerPlayerBridge) player).bridge$recordSelectedDrop(dropped);
        return dropped;
    }
}
