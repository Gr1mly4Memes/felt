package gr1mly4memes.felt.bridge.core.world.level.block.entity;

import org.bukkit.potion.PotionEffect;

public interface BeaconBlockEntityBridge {

    PotionEffect bridge$getPrimaryEffect();

    PotionEffect bridge$getSecondaryEffect();

    default PotionEffect getPrimaryEffect() {
        return this.bridge$getPrimaryEffect();
    }

    default PotionEffect getSecondaryEffect() {
        return this.bridge$getSecondaryEffect();
    }

}
