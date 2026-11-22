package gr1mly4memes.felt.mixin.core.world.clock;

import gr1mly4memes.felt.bridge.core.world.clock.ClockInstanceBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.clock.ServerClockManager$ServerClockInstance")
public interface ClockInstanceAccessor extends ClockInstanceBridge {
    @Override
    @Accessor("totalTicks")
    long bridge$totalTicks();

    @Override
    @Accessor("totalTicks")
    void bridge$totalTicks(long ticks);

    @Override
    @Accessor("timeMarkers")
    java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.clock.ClockTimeMarker>, net.minecraft.world.clock.ClockTimeMarker> bridge$timeMarkers();
}
