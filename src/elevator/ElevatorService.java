/*
 * Created by Zhen Guan
 * Student number: 202191382
 * Email: zguan@mun.ca
 */

package elevator;

import elevator.simulate.Clock;
import elevator.simulate.Logger;

import java.util.ArrayList;
import java.util.List;

public class ElevatorService {
    protected ArrayList<Elevator> elevators = new ArrayList<>();
    protected int numElevators;
    protected int elevatorCapacity;

    public ElevatorService(List<Floor> floors, int numElevators, int elevatorCapacity, SimulationConfig config) {
        this.numElevators = numElevators;
        this.elevatorCapacity = elevatorCapacity;

        for (int i = 0; i < numElevators; i++) {
            Clock clock = new Clock();
            elevators.add(
                new Elevator(i, elevatorCapacity, clock, config, floors)
            );
        }
    }

    public ArrayList<Elevator> getElevators() {
        return new ArrayList<>(elevators);
    }

    public void start() {
        for (Elevator elevator : elevators) {
            elevator.startService();
        }
    }

    public void waitForStop() {
        for (Elevator elevator : elevators) {
            elevator.waitForStop();
        }
    }
}
