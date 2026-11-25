package gr1mly4memes.felt.mixin.core.world.timeline;

import com.google.common.collect.ImmutableMap;
import gr1mly4memes.felt.bridge.core.world.timeline.TimelineBuilderBridge;
import java.util.Map;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.timeline.AttributeTrack;
import net.minecraft.world.timeline.Timeline;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Timeline.Builder.class)
public abstract class TimelineBuilderMixin implements TimelineBuilderBridge {
    @Shadow @Final private ImmutableMap.Builder<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks;

    @Override
    public void bridge$addTracks(Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks) {
        this.tracks.putAll(tracks);
    }
}
