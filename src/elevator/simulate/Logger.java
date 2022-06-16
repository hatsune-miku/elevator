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

    public static synchronized void info(String message, Object... args) {
        log("INFO", message, args);
    }

    public static synchronized void error(String message, Object... args) {
        log("ERROR", message, args);
    }

    public static synchronized void verbose(String message, Object... args) {
        log("VERBOSE", message, args);
    }

    private static void log(String level, String message, Object... args) {
        System.out.printf(
            "%s [%s] %s\n".formatted(new Date().toString(), level, message),
            args
        );
    }
}
