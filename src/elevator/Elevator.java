/*
 * Created by Zhen Guan
 * Student number: 202191382
 * Email: zguan@mun.ca
 */

package elevator;

import elevator.simulate.Clock;
import elevator.simulate.Logger;
import elevator.simulate.UniformRandom;

import java.util.*;
import java.util.function.Function;

/**
 * The implementation of the elevator system.
 * Includes all running logics.
 * Driven by the ticks of {@link Clock}.
 */
public class Elevator implements Clock.OnTickListener, Clock.OnTickDoneListener {
    public enum Direction {
        UP, DOWN, IDLE
    }

    public enum Status {
        // The status that the elevator is waiting for next order.
        IDLE,

        // The processes that the elevator is moving.
        MOVING_UP, MOVING_DOWN,

        // The processes of opening/closing the door.
        OPENING_DOOR, CLOSING_DOOR,

        // The process of loading/unloading passengers.
        WAITING_BOARDING,

        // This status is reserved for further use.
        ERROR
    }

    // Clock.
    protected final Clock clock;

    // Status for floors.
    List<Floor> floors;

    // floor X has Y visitors.
    int[] floorsToVisitCount;
    protected int currentFloor;
    protected int destinationFloor;

    // Status for elevator running.
    protected Status status;
    protected Direction direction;
    protected boolean isDoorOpen;
    protected boolean isMoving;

    // Status for statistics.
    protected int ridersServed;
    protected int ridersRequested;
    protected int ridersInside;

    protected final int id;
    protected final int capacity;
    protected final SimulationConfig config;

    protected int randomRequestCounter = 0;

    // Utility variables.
    UniformRandom random;


    public Elevator(int id, int capacity, Clock clock, SimulationConfig elevatorConfig, List<Floor> floors) {
        this.id = id;
        this.capacity = capacity;
        this.config = elevatorConfig;
        this.clock = clock;

        this.floors = floors;
        this.currentFloor = 0;
        this.destinationFloor = -1;
        this.floorsToVisitCount = new int[floors.size()];
        Arrays.fill(floorsToVisitCount, 0);

        this.status = Status.IDLE;
        this.direction = Direction.IDLE;
        this.isDoorOpen = false;
        this.isMoving = false;

        this.ridersInside = 0;
        this.ridersRequested = 0;
        this.ridersServed = 0;

        this.random = new UniformRandom();

        this.clock.addOnTickListener(this);
        this.clock.addOnTickDoneListener(this);
    }

    protected boolean isFloorIndexValid(int index) {
        return index >= 0 && index < floors.size();
    }

    /**
     * Push the floor button.
     *
     * @param floorIndex
     */
    public void onRequestVisit(int floorIndex) {
        if (!isFloorIndexValid(floorIndex)) {
            return;
        }
        ++floorsToVisitCount[floorIndex];
    }

    /**
     * Start the elevator service.
     * Start to accept passengers.
     */
    public void startService() {
        if (!clock.start(config.runningSeconds(), config.millisecondsPerSecond())) {
            Logger.error("Elevator #%d failed to start.", id);
        }
    }

    /**
     * Blocks until the running time is up or the elevator is stopped.
     */
    public void waitForStop() {
        clock.waitForStop();
    }

    @Override
    public void onTick(boolean isLastTick, boolean isSkippingTick) {
        if (isDoorOpen() && isMoving()) {
            abort("Elevator is moving while door is open.");
        }

        // Generate two random floor requests (outside elevator).
        if (++randomRequestCounter >= config.randomRequestTickGenerator().call()) {
            int floorIndex0 = random.nextInt(currentFloor, floors.size() - 1);
            int floorIndex1 = random.nextInt(0, currentFloor);

            if (floorIndex0 != currentFloor) {
                floors.get(floorIndex0).setUpRequested(true);
                Logger.verbose("Elevator #%d: new random request, floor #%d, up.",
                    id, floorIndex0);
            }

            if (floorIndex1 != currentFloor) {
                floors.get(floorIndex1).setDownRequested(true);
                Logger.verbose("Elevator #%d: new random request, floor #%d, down.",
                    id, floorIndex1);
            }

            randomRequestCounter = 0;
        }

        if (isSkippingTick) {
            return;
        }

        switch (status) {
            case IDLE -> {
                handleIdle();
            }

            case MOVING_UP -> {
                clock.skip(config.moveUpToNeighbourFloorSeconds());
                if (currentFloor == floors.size() - 1) {
                    abort("Elevator is moving to an invalid floor.");
                }
                ++currentFloor;
                HardwareControl.moveUp(id);
                setStatus(Status.IDLE);
            }

            case MOVING_DOWN -> {
                clock.skip(config.moveDownToNeighbourFloorSeconds());
                if (currentFloor == 0) {
                    abort("Elevator is moving to an invalid floor.");
                }
                --currentFloor;
                HardwareControl.moveDown(id);
                setStatus(Status.IDLE);
            }

            case OPENING_DOOR -> {
                clock.skip(config.openDoorSeconds());
                setDoorOpen(true);
                setStatus(Status.WAITING_BOARDING);
            }

            case CLOSING_DOOR -> {
                clock.skip(config.closeDoorSeconds());
                setDoorOpen(false);
                setStatus(Status.IDLE);
            }

            case WAITING_BOARDING -> {
                clock.skip(config.waitOnBoardSeconds());
                setStatus(Status.CLOSING_DOOR);
                handleWaitingOnBoard();
            }

            case ERROR -> {
                abort("Elevator is in error state.");
            }
        }
    }

    @Override
    public void onTickDone(Reason reason) {
        Logger.loggingMutex.lock();

        Logger.info("Elevator #%d finished (%s). Report: ", id, reason.getDescription());

        int requested = getRidersRequested();
        int served = getRidersServed();

        Logger.info("\tElevator #%d: ", id);
        Logger.info("\t\t     Riders requested: %d", requested);
        Logger.info("\t\t        Riders served: %d", served);
        Logger.info("\t\t Riders not picked up: %d", requested - served);
        Logger.info("\t\t   Average serve time: %.2f seconds", (float) config.runningSeconds() / served);

        Logger.loggingMutex.unlock();
    }

    private void handleWaitingOnBoard() {
        // How many persons left?
        int left = floorsToVisitCount[currentFloor];
        floorsToVisitCount[currentFloor] = 0;
        onRidersLeaved(left);

        // How many persons want to enter the elevator?
        int requested = config.randomRiderGenerator().call();

        // How many available spaces in the elevator?
        int acceptable = capacity - ridersInside;

        // How many persons can enter the elevator?
        int served = Math.min(requested, acceptable);

        onRidersServed(served);
        onRidersRequested(requested);

        Logger.verbose("Elevator #%d: on floor #%d, %d left, %d requested, %d served.",
            id, currentFloor, left, requested, served);

        // New riders are requesting for their destinations.
        // Some guys may select an arbitrary floor regardless of the direction.
        for (int i = 0; i < served; i++) {
            int floorIndex;

            if (getDirection() == Direction.UP) {
                floorIndex = random.nextInt(currentFloor, floors.size() - 1);
            } else {
                floorIndex = random.nextInt(0, currentFloor);
            }
            onRequestVisit(floorIndex);
            Logger.verbose("Elevator #%d: a rider requested to visit floor #%d.",
                id, floorIndex);
        }
    }

    private void handleIdle() {
        // Have destination?
        if (destinationFloor != -1) {
            if (currentFloor == destinationFloor) {
                // Arrived. Clear destination and open door.
                Floor floor = floors.get(currentFloor);
                destinationFloor = -1;

                // Take off the request.
                if (getDirection() == Direction.UP) {
                    floor.setUpRequested(false);
                } else {
                    floor.setDownRequested(false);
                }

                // Unlock both reentrant locks - as we do not know
                // which one is locked in directionless case.
                // Only to ensure that we do not hold any locks on it.
                if (floor.tryLockUp()) {
                    floor.unlockUp();
                }
                if (floor.tryLockDown()) {
                    floor.unlockDown();
                }
                // Update direction if necessary.
                if (currentFloor == 0) {
                    Logger.verbose("Elevator #%d: reached bound. Changing direction.", id);
                    setDirection(Direction.UP);
                }
                else if (currentFloor == floors.size() - 1) {
                    Logger.verbose("Elevator #%d: reached bound. Changing direction.", id);
                    setDirection(Direction.DOWN);
                }

                // Update status to open the door.
                setStatus(Status.OPENING_DOOR);


            } else if (currentFloor > destinationFloor) {
                // Move down.
                setDirection(Direction.DOWN);
                setStatus(Status.MOVING_DOWN);

            } else {
                // Move up.
                setDirection(Direction.UP);
                setStatus(Status.MOVING_UP);
            }
            return;
        }

        // Otherwise, find a destination according to the direction.
        destinationFloor = findAndLockNearestFloorIf(
            (i) -> {
                Floor floor = floors.get(i);
                if (direction == Direction.UP) {
                    return currentFloor <= i && floor.isUpRequested();
                } else if (direction == Direction.DOWN) {
                    return currentFloor >= i && floor.isDownRequested();
                } else {
                    return floor.isUpRequested() || floor.isDownRequested();
                }
            }
        );

        // Keep idle if no such floor.
        if (destinationFloor == -1) {
            setDirection(Direction.IDLE);
            setStatus(Status.IDLE);
        } else {
            handleIdle();
        }
    }

    /**
     * Find and lock the nearest outside-requested floor, according to current direction.
     *
     * @param condition The condition to qualify the floor index.
     * @return The floor index. -1 if no such floor.
     */
    private int findAndLockNearestFloorIf(Function<Integer, Boolean> condition) {
        if (direction == Direction.UP || direction == Direction.IDLE) {
            for (int i = currentFloor; i < floors.size(); ++i) {
                Floor floor = floors.get(i);

                // There are visitors, or the floor is requested.
                if (
                    floorsToVisitCount[i] > 0
                    || (condition.apply(i) && floor.tryLockUp())
                ) {
                    return i;
                }
            }
        }
        if (direction == Direction.DOWN || direction == Direction.IDLE) {
            for (int i = currentFloor; i >= 0; --i) {
                Floor floor = floors.get(i);

                // There are visitors, or the floor is requested.
                if (
                    floorsToVisitCount[i] > 0
                    || (condition.apply(i) && floor.tryLockDown())
                ) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isDoorOpen() {
        return isDoorOpen;
    }

    public void setDoorOpen(boolean doorOpen) {
        if (doorOpen && isMoving()) {
            abort("Elevator is trying to open door while moving.");
        }
        if (doorOpen) {
            HardwareControl.openDoor(id);
            Logger.verbose("Elevator #%d: Door opening.", id);
        } else {
            HardwareControl.closeDoor(id);
            Logger.verbose("Elevator #%d: Door closing. Elevator moving.", id);
        }
        isDoorOpen = doorOpen;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        if (moving && isDoorOpen()) {
            abort("Elevator is trying to move while door is open.");
        }
        isMoving = moving;
    }

    public void setDirection(Direction newDirection) {
        this.direction = newDirection;

        if (newDirection == Direction.UP) {
            Logger.verbose("Elevator #%d: Elevator going up.", id);
        } else if (newDirection == Direction.DOWN) {
            Logger.verbose("Elevator #%d: Elevator going down.", id);
        }
    }

    public boolean isFull() {
        return ridersInside == capacity;
    }

    public boolean isEmpty() {
        return ridersInside == 0;
    }

    public int getRidersServed() {
        return ridersServed;
    }

    public void onRidersServed(int riders) {
        this.ridersServed += riders;
        this.ridersInside += riders;
    }

    public int getRidersRequested() {
        return ridersRequested;
    }

    public void onRidersRequested(int riders) {
        this.ridersRequested += riders;
    }

    public void onRidersLeaved(int riders) {
        this.ridersInside -= riders;
    }

    private void abort(String reason) {
        HardwareControl.abort(id);
        Logger.error("Severe error: %s", reason);
        while (true) {
            System.exit(Errors.ERROR_CODE_INVALID_STATUS);
        }
    }
}
