package gr1mly4memes.felt.bridge.core.world.timeline;

import java.util.Map;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.timeline.AttributeTrack;

public interface TimelineBuilderBridge {
    void bridge$addTracks(Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks);
}
