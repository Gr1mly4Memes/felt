package gr1mly4memes.felt.common.mixin.core.world.damagesource;

import gr1mly4memes.felt.common.bridge.core.world.damagesource.CombatEntryBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.CombatEntry;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CombatEntry.class)
public class CombatEntryMixin implements CombatEntryBridge {

    private Component arclight$deathMessage;

    @Override
    public void bridge$setDeathMessage(Component component) {
        this.arclight$deathMessage = component;
    }

    @Override
    public Component bridge$getDeathMessage() {
        return this.arclight$deathMessage;
    }
}
