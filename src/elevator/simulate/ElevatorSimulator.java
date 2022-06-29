/*
 * Created by Zhen Guan
 * Student number: 202191382
 * Email: zguan@mun.ca
 */

package elevator.simulate;

import elevator.ElevatorService;
import elevator.Floor;
import elevator.SimulationConfig;

import java.util.ArrayList;
import java.util.List;

public final class ElevatorSimulator {
    private final ElevatorService service;
    private final List<Floor> floors;
    private final int numElevators;
    private final int elevatorCapacity;

    // Constructor
    public ElevatorSimulator(int numFloors, int numElevators, int elevatorCapacity, SimulationConfig config) {
        this.numElevators = numElevators;
        this.elevatorCapacity = elevatorCapacity;

        // Generate floors.
        this.floors = new ArrayList<>();
        for (int i = 0; i < numFloors; i++) {
            floors.add(new Floor());
        }

        // Create elevators.
        this.service = new ElevatorService(floors, numElevators, elevatorCapacity, config);
    }

    /**
     * Run and block until the simulation is complete.
     */
    public void run() {
        Logger.info(
            "=== Simulation started with elevators=%d, capacity=%d ===",
            numElevators, elevatorCapacity
        );

        service.start();
        service.waitForStop();

        Logger.info(
            "=== Simulation ended ===",
            numElevators, elevatorCapacity
        );
    }
}
