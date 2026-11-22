package gr1mly4memes.felt.mod.util.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FeltI18nLogger {

    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }
}
