package gr1mly4memes.felt.bridge.core.world.entity;

public interface ItemBasedSteeringBridge {

    default void setBoostTicks(int ticks) {
        throw new UnsupportedOperationException("Implemented by ItemBasedSteeringMixin");
    }
}
