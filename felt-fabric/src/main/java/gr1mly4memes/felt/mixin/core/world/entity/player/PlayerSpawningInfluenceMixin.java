package gr1mly4memes.felt.mixin.core.world.entity.player;

import gr1mly4memes.felt.bridge.core.world.entity.player.SpawningInfluenceBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerSpawningInfluenceMixin implements SpawningInfluenceBridge {
    @Unique private boolean felt$affectsSpawning = true;
    @Override public boolean bridge$affectsSpawning() { return this.felt$affectsSpawning; }
    @Override public void bridge$setAffectsSpawning(boolean affects) { this.felt$affectsSpawning = affects; }
}
