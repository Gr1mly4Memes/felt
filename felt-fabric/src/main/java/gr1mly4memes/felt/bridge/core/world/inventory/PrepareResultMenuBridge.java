package gr1mly4memes.felt.bridge.core.world.inventory;

/** Defers per-write prepare events until the native item-combiner calculation completes. */
public interface PrepareResultMenuBridge {

    boolean bridge$isPrepareResultDeferred();

    void bridge$setPrepareResultDeferred(boolean deferred);
}
