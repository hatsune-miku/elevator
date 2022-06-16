/*
 * Created by Zhen Guan
 * Student number: 202191382
 * Email: zguan@mun.ca
 */

package elevator.simulate;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Clock {
    public interface OnTickListener {
        void onTick(boolean isLastTick, boolean isSkippingTick);
    }

    public interface OnTickDoneListener {
        void onTickDone(Reason reason);

        enum Reason {
            TIME_IS_UP("Time is up"),
            CANCELLED_BY_USER("Cancelled by user");

            private final String description;

            Reason(String description) {
                this.description = description;
            }

            public String getDescription() {
                return description;
            }
        }
    }

    protected int tick;
    protected int tickSkipRemaining;

    protected int totalTicks;
    protected boolean isRunning;
    protected ScheduledExecutorService executorService;
    protected ScheduledFuture<?> future;
    protected ArrayList<OnTickListener> onTickListeners;
    protected ArrayList<OnTickDoneListener> onTickDoneListeners;

    public Clock() {
        tick = 0;
        tickSkipRemaining = 0;
        totalTicks = 0;
        onTickListeners = new ArrayList<>();
        onTickDoneListeners = new ArrayList<>();
    }

    /**
     * @param listener
     * @return true if the listener is added successfully,
     * false if the listener was already added.
     */
    public boolean addOnTickListener(OnTickListener listener) {
        if (onTickListeners.contains(listener)) {
            return false;
        }
        return onTickListeners.add(listener);
    }

    /**
     * @param listener
     * @return true if the listener is added successfully,
     * false if the listener was already added.
     */
    public boolean addOnTickDoneListener(OnTickDoneListener listener) {
        if (onTickDoneListeners.contains(listener)) {
            return false;
        }
        return onTickDoneListeners.add(listener);
    }

    /**
     * Start the clock and start counting ticks.
     *
     * @param runningSeconds          the total time in seconds.
     * @param millisecondsOfOneSecond define the milliseconds of one second.
     * @return true if the clock is started successfully,
     * false if the clock is already started.
     */
    public synchronized boolean start(int runningSeconds, int millisecondsOfOneSecond) {
        if (isRunning) {
            return false;
        }
        this.totalTicks = runningSeconds;
        executorService = Executors.newSingleThreadScheduledExecutor();
        future = executorService.scheduleAtFixedRate(
            new Ticker(this), 0, millisecondsOfOneSecond, TimeUnit.MILLISECONDS
        );
        isRunning = true;
        return true;
    }

    public synchronized boolean start(int runningSeconds) {
        return start(runningSeconds, 1000);
    }

    /**
     * Force the background thread to stop ticking immediately and notify to listeners.
     */
    public synchronized void stop() {
        stopNoNotify();
        onTickDoneListeners.forEach(
            listener -> listener.onTickDone(
                OnTickDoneListener.Reason.CANCELLED_BY_USER)
        );
    }

    /**
     * Blocks until running time is up.
     */
    public void waitForStop() {
        try {
            future.get();
        } catch (Exception e) {
            // ignored.
        }
    }

    private void stopNoNotify() {
        future.cancel(true);
        executorService.shutdown();
        executorService = null;
        future = null;
        isRunning = false;
    }

    /**
     * Destructive method.
     * @return the current tick.
     */
    private int tick() {
        return ++tick;
    }

    public int getTick() {
        return tick;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    /**
     * Destructive method.
     * @param seconds the number of seconds to be marked as `skipped`.
     */
    public synchronized void skip(int seconds) {
        tickSkipRemaining += seconds;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private static class Ticker implements Runnable {
        Clock instance;

        public Ticker(Clock instance) {
            this.instance = instance;
        }

        @Override
        public void run() {
            if (!instance.isRunning()) {
                return;
            }

            instance.tick();
            boolean timeUp = instance.getTick() >= instance.getTotalTicks();
            boolean isSkippingTick = --instance.tickSkipRemaining > 0;

            instance.onTickListeners.forEach(
                listener -> {
                    try {
                        listener.onTick(timeUp, isSkippingTick);
                    }
                    catch (Exception e) {
                        Logger.error("Error in elevator: %s.", e);
                        Logger.error("\tStopping the elevator.");
                        instance.stopNoNotify();
                    }
                }
            );

            // Time is up?
            if (timeUp) {
                instance.onTickDoneListeners.forEach(
                    listener -> listener.onTickDone(
                        OnTickDoneListener.Reason.TIME_IS_UP)
                );
                instance.stopNoNotify();
            }
        }
    }
}
