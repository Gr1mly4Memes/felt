package gr1mly4memes.felt.mixin.core.world.entity.player;

import gr1mly4memes.felt.bridge.core.world.entity.player.BedSleepingProblemBridge;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.BedSleepingProblem.class)
public abstract class Player_BedSleepingProblemMixin implements BedSleepingProblemBridge {
    private PlayerBedEnterEvent.BedEnterResult felt$bedResult;
    @Nullable
    private BedRule felt$bedRule;

    public PlayerBedEnterEvent.BedEnterResult bukkit() {
        if (felt$bedResult != null) return felt$bedResult;
        if ((Object) this == Player.BedSleepingProblem.TOO_FAR_AWAY) return PlayerBedEnterEvent.BedEnterResult.TOO_FAR_AWAY;
        if ((Object) this == Player.BedSleepingProblem.NOT_SAFE) return PlayerBedEnterEvent.BedEnterResult.NOT_SAFE;
        return PlayerBedEnterEvent.BedEnterResult.OTHER_PROBLEM;
    }

    @Override
    public void bridge$setBedResult(PlayerBedEnterEvent.BedEnterResult result) {
        this.felt$bedResult = result;
    }

    @Override
    @Nullable
    public BedRule bridge$getBedRule() {
        return this.felt$bedRule;
    }

    @Override
    public void bridge$setBedRule(@Nullable BedRule bedRule) {
        this.felt$bedRule = bedRule;
    }
}
