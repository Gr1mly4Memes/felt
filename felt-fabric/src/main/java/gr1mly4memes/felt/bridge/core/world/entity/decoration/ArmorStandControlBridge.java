package gr1mly4memes.felt.bridge.core.world.entity.decoration;

public interface ArmorStandControlBridge {
    void bridge$equipmentChanged();
    boolean bridge$canMove();
    void bridge$setCanMove(boolean move);
    boolean bridge$canTick();
    void bridge$setCanTick(boolean tick);
}
