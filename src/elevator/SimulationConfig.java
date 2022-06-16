/*
 * Created by Zhen Guan
 * Student number: 202191382
 * Email: zguan@mun.ca
 */

package elevator;

import elevator.simulate.UniformRandom;

public record SimulationConfig(
    int runningSeconds,
    int millisecondsPerSecond,
    int moveUpToNeighbourFloorSeconds,
    int moveDownToNeighbourFloorSeconds,
    int openDoorSeconds,
    int closeDoorSeconds,
    int waitOnBoardSeconds,
    Callable<Integer> randomRiderGenerator,
    Callable<Integer> randomRequestTickGenerator
) {
    protected static final int DEFAULT_MOVE_UP_TO_NEIGHBOUR_FLOOR_SECONDS = 5;
    protected static final int DEFAULT_MOVE_DOWN_TO_NEIGHBOUR_FLOOR_SECONDS = 5;
    protected static final int DEFAULT_OPEN_DOOR_SECONDS = 0;
    protected static final int DEFAULT_CLOSE_DOOR_SECONDS = 0;
    protected static final int DEFAULT_WAIT_ON_BOARD_SECONDS = 15;

    protected static final Callable<Integer> defaultRandomRiderGenerator = () -> {
        return (Integer) new UniformRandom().nextRandomItemOf(new Integer[] {
            0, 0, 0, 1, 1, 1, 1, 1, 1, 2
        });
    };

    protected static final Callable<Integer> defaultRandomRequestTickGenerator = () -> {
        return new UniformRandom().nextInt(20, 121);
    };

    public static SimulationConfig defaultConfig(int runningSeconds, int millisecondsPerSecond) {
        return new SimulationConfig(
            runningSeconds, millisecondsPerSecond,
            DEFAULT_MOVE_UP_TO_NEIGHBOUR_FLOOR_SECONDS,
            DEFAULT_MOVE_DOWN_TO_NEIGHBOUR_FLOOR_SECONDS,
            DEFAULT_OPEN_DOOR_SECONDS,
            DEFAULT_CLOSE_DOOR_SECONDS,
            DEFAULT_WAIT_ON_BOARD_SECONDS,
            defaultRandomRiderGenerator,
            defaultRandomRequestTickGenerator
        );
    }

    interface Callable<T> {
        T call();
    }
}
