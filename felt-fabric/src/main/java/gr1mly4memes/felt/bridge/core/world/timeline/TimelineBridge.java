package gr1mly4memes.felt.bridge.core.world.timeline;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.timeline.AttributeTrack;

public interface TimelineBridge {
    Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> bridge$tracks();
    Map<ResourceKey<ClockTimeMarker>, ?> bridge$timeMarkers();
}
