package gr1mly4memes.felt.bridge.core.world.damagesource;

import net.minecraft.network.chat.Component;

public interface CombatEntryBridge {

    void bridge$setDeathMessage(Component component);

    Component bridge$getDeathMessage();
}
