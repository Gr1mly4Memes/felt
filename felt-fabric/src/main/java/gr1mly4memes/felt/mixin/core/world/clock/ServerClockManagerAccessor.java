package gr1mly4memes.felt.mixin.core.world.clock;

import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerClockManager.class)
public interface ServerClockManagerAccessor {
    @Accessor("clocks")
    java.util.Map<Holder<WorldClock>, Object> felt$clocks();

    @Invoker("modifyClock")
    void felt$modifyClock(Holder<WorldClock> clock, Consumer<?> action);
}
