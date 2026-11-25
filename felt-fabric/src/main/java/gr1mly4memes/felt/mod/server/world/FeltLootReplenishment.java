package gr1mly4memes.felt.mod.server.world;

import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;

public final class FeltLootReplenishment {
    private FeltLootReplenishment() {}
    private static final ThreadLocal<RandomizableContainer> FORCE_CLEAR = new ThreadLocal<>();
    public static boolean forceClear(RandomizableContainer container) { return FORCE_CLEAR.get() == container; }
    public static void unpackAndClear(RandomizableContainer container, Player player) {
        RandomizableContainer previous = FORCE_CLEAR.get();
        FORCE_CLEAR.set(container);
        try { container.unpackLootTable(player); }
        finally {
            container.setLootTable(null);
            if (previous == null) FORCE_CLEAR.remove(); else FORCE_CLEAR.set(previous);
        }
    }
}
