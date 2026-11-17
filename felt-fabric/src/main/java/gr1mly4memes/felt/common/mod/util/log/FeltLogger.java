package gr1mly4memes.felt.common.mod.util.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FeltLogger {

    private FeltLogger() {
    }

    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }
}
