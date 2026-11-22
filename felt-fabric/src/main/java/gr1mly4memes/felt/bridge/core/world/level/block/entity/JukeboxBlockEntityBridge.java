package gr1mly4memes.felt.bridge.core.world.level.block.entity;

public interface JukeboxBlockEntityBridge {
    default void setSongItemWithoutPlaying(net.minecraft.world.item.ItemStack itemStack, long ticksSinceSongStarted) {
        throw new UnsupportedOperationException("Implemented by JukeboxBlockEntityMixin");
    }
}
