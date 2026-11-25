package gr1mly4memes.felt.mixin.core.world.timeline;

import gr1mly4memes.felt.bridge.core.world.timeline.TimelineBridge;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.timeline.AttributeTrack;
import net.minecraft.world.timeline.Timeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Timeline.class)
public abstract class TimelineMixin implements TimelineBridge {
    @Override @Accessor("tracks")
    public abstract Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> bridge$tracks();

    @Override @Accessor("timeMarkers")
    public abstract Map<ResourceKey<ClockTimeMarker>, ?> bridge$timeMarkers();

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Timeline other)) {
            return false;
        }
        var self = (Timeline) (Object) this;
        var bridge = (TimelineBridge) other;
        return self.clock().equals(other.clock())
            && self.periodTicks().equals(other.periodTicks())
            && this.bridge$tracks().equals(bridge.bridge$tracks())
            && this.bridge$timeMarkers().equals(bridge.bridge$timeMarkers());
    }

    @Override
    public int hashCode() {
        var self = (Timeline) (Object) this;
        return Objects.hash(self.clock(), self.periodTicks(), this.bridge$tracks(), this.bridge$timeMarkers());
    }
}
