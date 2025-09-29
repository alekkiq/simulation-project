package simu.config;

import java.util.Objects;

/**
 * Configuration class for simulation parameters.
 * This class can be expanded to include various configuration settings
 */
public class SimulationConfig {

    // Resource counts
    private int mechanics;
    private int washers;

    // How long the simulation runs (simulation clock time)
    private double simulationTime;

    // Basic distribution parameters (kept primitive on purpose)
    // Arrival process: Negative Exponential mean
    private double arrivalMean;

    // Mechanic service time: Normal distribution (mean + stdDev)
    private double mechanicServiceMean;
    private double mechanicServiceStdDev;

    // Washer service time: Negative Exponential mean
    private double washerServiceMean;

    /**
     * No-arg constructor with some safe default values.
     */
    public SimulationConfig() {
        this.mechanics = 1;
        this.washers = 1;
        this.simulationTime = 10_000.0;
        this.arrivalMean = 5.0;
        this.mechanicServiceMean = 30.0;
        this.mechanicServiceStdDev = 5.0;
        this.washerServiceMean = 40.0;
    }

    /**
     * Full constructor for quick manual setup (e.g. from a simple form).
     */
    public SimulationConfig(int mechanics,
                            int washers,
                            double simulationTime,
                            double arrivalMean,
                            double mechanicServiceMean,
                            double mechanicServiceStdDev,
                            double washerServiceMean) {
        this.mechanics = mechanics;
        this.washers = washers;
        this.simulationTime = simulationTime;
        this.arrivalMean = arrivalMean;
        this.mechanicServiceMean = mechanicServiceMean;
        this.mechanicServiceStdDev = mechanicServiceStdDev;
        this.washerServiceMean = washerServiceMean;
    }

    // Getters (no setters unless you want mutability from a future UI form)
    public int getMechanics() { return mechanics; }
    public int getWashers() { return washers; }
    public double getSimulationTime() { return simulationTime; }
    public double getArrivalMean() { return arrivalMean; }
    public double getMechanicServiceMean() { return mechanicServiceMean; }
    public double getMechanicServiceStdDev() { return mechanicServiceStdDev; }
    public double getWasherServiceMean() { return washerServiceMean; }

    /**
     * Simple validation; call before starting the engine.
     * Throws IllegalArgumentException on invalid input.
     */
    public void validate() {
        if (mechanics <= 0) throw new IllegalArgumentException("mechanics must be > 0");
        if (washers <= 0) throw new IllegalArgumentException("washers must be > 0");
        if (simulationTime <= 0) throw new IllegalArgumentException("simulationTime must be > 0");
        if (arrivalMean <= 0) throw new IllegalArgumentException("arrivalMean must be > 0");
        if (mechanicServiceMean <= 0) throw new IllegalArgumentException("mechanicServiceMean must be > 0");
        if (mechanicServiceStdDev < 0) throw new IllegalArgumentException("mechanicServiceStdDev must be >= 0");
        if (washerServiceMean <= 0) throw new IllegalArgumentException("washerServiceMean must be > 0");
    }

    @Override
    public String toString() {
        return "SimulationConfig{" +
                "mechanics=" + mechanics +
                ", washers=" + washers +
                ", simulationTime=" + simulationTime +
                ", arrivalMean=" + arrivalMean +
                ", mechanicServiceMean=" + mechanicServiceMean +
                ", mechanicServiceStdDev=" + mechanicServiceStdDev +
                ", washerServiceMean=" + washerServiceMean +
                '}';
    }
}