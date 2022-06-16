import elevator.SimulationConfig;
import elevator.simulate.ElevatorSimulator;
import elevator.simulate.Logger;


public class CSFElevatorSimulator {
    public static void main(String[] args) {
        int numFloors = 5;

        int numElevatorsSmall = 4;
        int elevatorCapacitySmall = 1;

        int numElevatorsLarge = 2;
        int elevatorCapacityLarge = 2;

        int simulationSeconds = 7200;
        int millisecondsPerSecond = 1;

        SimulationConfig config = SimulationConfig.defaultConfig(
            simulationSeconds, millisecondsPerSecond
        );

        ElevatorSimulator smallElevatorSimulator = new ElevatorSimulator(
            numFloors, numElevatorsSmall, elevatorCapacitySmall, config
        );
        ElevatorSimulator largeElevatorSimulator = new ElevatorSimulator(
            numFloors, numElevatorsLarge, elevatorCapacityLarge, config
        );

        try {
            // smallElevatorSimulator.run();
            largeElevatorSimulator.run();
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }
}
