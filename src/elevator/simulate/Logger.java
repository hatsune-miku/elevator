/*
 * Created by Zhen Guan
 * Student number: 202191382
 * Email: zguan@mun.ca
 */

package elevator.simulate;

import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class Logger {
    public static final ReentrantLock loggingMutex = new ReentrantLock();
    protected static Level loggingLevel = Level.INFO;

    public static void setLoggingLevel(Level level) {
        loggingLevel = level;
    }

    public static synchronized void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public static synchronized void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    public static synchronized void verbose(String message, Object... args) {
        log(Level.VERBOSE, message, args);
    }

    private static void log(Level level, String message, Object... args) {
        if (level.getValue() > loggingLevel.getValue()) {
            return;
        }
        System.out.printf(
            "%s [%s] %s\n".formatted(
                new Date().toString(), level.getDescription(), message
            ),
            args
        );
    }

    public enum Level {
        NONE("NONE", 0),
        INFO("INFO", 1),
        ERROR("ERROR", 2),
        VERBOSE("VERBOSE", 3);

        String description;
        private int value;

        Level(String description, int value) {
            this.description = description;
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }
}
