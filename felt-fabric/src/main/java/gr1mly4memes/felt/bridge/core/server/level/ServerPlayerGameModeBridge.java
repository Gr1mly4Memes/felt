package gr1mly4memes.felt.bridge.core.server.level;

import gr1mly4memes.felt.mod.util.FeltCaptures;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public interface ServerPlayerGameModeBridge {

    /**
     * Returns this invocation's dispatched event on native success or Bukkit cancellation.
     * Returns null for an unchanged mode or a native/mod veto without a Bukkit cancellation;
     * callers must not infer success merely from an event having been dispatched.
     * A cancellation message belongs to the command/switcher caller, not the plugin setter.
     */
    @Nullable
    PlayerGameModeChangeEvent bridge$withGameModeChange(PlayerGameModeChangeEvent.Cause cause, @Nullable Component cancelMessage, BooleanSupplier nativeChange);

    boolean bridge$isFiredInteract();

    void bridge$setFiredInteract(boolean b);

    boolean bridge$getInteractResult();

    void bridge$setInteractResult(boolean b);

    void bridge$handleBlockDrop(FeltCaptures.BlockBreakEventContext breakEventContext, BlockPos pos);

    BlockPos bridge$getInteractPosition();

    InteractionHand bridge$getInteractHand();

    ItemStack bridge$getInteractItemStack();
}
