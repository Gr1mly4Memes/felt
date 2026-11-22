package gr1mly4memes.felt.bridge.core.world.clock;

/** Read-only access to the actual prewrite clock value; no cloned clock or synthetic marker. */
public interface ClockInstanceBridge {
    long bridge$totalTicks();

    void bridge$totalTicks(long ticks);

    java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.clock.ClockTimeMarker>, net.minecraft.world.clock.ClockTimeMarker> bridge$timeMarkers();
}
