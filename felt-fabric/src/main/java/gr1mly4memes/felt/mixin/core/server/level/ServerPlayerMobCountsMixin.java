package gr1mly4memes.felt.mixin.core.server.level;

import gr1mly4memes.felt.bridge.core.server.level.PlayerMobCountsBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMobCountsMixin implements PlayerMobCountsBridge {
    @Unique private final int[] felt$mobCounts = new int[MobCategory.values().length];
    @Unique private final int[] felt$mobBackoffCounts = new int[MobCategory.values().length];
    @Override public int[] bridge$mobCounts() { return this.felt$mobCounts; }
    @Override public int[] bridge$mobBackoffCounts() { return this.felt$mobBackoffCounts; }
}
