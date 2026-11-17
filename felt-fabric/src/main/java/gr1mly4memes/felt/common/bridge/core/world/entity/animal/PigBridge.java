package gr1mly4memes.felt.common.bridge.core.world.entity.animal;

import net.minecraft.world.entity.LightningBolt;

public interface PigBridge {

    LightningBolt bridge$getPigZapLightning();

    void bridge$clearPigZapLightning();

    void bridge$cancelPigZap();
}
