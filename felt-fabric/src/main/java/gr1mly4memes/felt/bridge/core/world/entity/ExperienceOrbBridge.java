package gr1mly4memes.felt.bridge.core.world.entity;

import gr1mly4memes.felt.mod.server.world.FeltExperience.Attribution;

public interface ExperienceOrbBridge {
    Attribution bridge$getAttribution();
    void bridge$setAttribution(Attribution attribution);

    void bridge$setValue(int value);
}
