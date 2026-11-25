package gr1mly4memes.felt.bridge.core.world.inventory;

public interface AnvilMenuBridge {
    boolean bridge$bypassEnchantmentLimit();
    void bridge$setBypassEnchantmentLimit(boolean bypass);

    void felt$allowZeroCost();

    boolean felt$isZeroCostAllowed();
}
