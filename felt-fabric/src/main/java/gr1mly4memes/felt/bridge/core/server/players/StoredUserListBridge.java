package gr1mly4memes.felt.bridge.core.server.players;

import java.util.Collection;

public interface StoredUserListBridge<V> {

    Collection<V> bridge$getValues();
}
