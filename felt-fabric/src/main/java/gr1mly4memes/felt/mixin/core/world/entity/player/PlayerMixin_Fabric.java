package gr1mly4memes.felt.mixin.core.world.entity.player;

import gr1mly4memes.felt.bridge.core.world.entity.player.PlayerBridge;
import gr1mly4memes.felt.mixin.core.world.entity.LivingEntityMixin_Fabric;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin_Fabric extends LivingEntityMixin_Fabric implements PlayerBridge {

    // @formatter:off
    @Shadow public abstract Abilities getAbilities();
    // @formatter:on
}
