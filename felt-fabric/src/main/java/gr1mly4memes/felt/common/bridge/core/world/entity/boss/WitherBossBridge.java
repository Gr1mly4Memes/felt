package gr1mly4memes.felt.common.bridge.core.world.entity.boss;

public interface WitherBossBridge {

    default void setCanTravelThroughPortals(boolean value) {
        throw new UnsupportedOperationException("Implemented by WitherBossMixin");
    }
}
