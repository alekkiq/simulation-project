package simu.config;

import java.util.Arrays;

/**
 * Configuration class for simulation parameters.
 * These will be inserted from the view by the user, and used
 * by the engine to configure the simulation parameters
 */
public class SimulationOptions {
    // simulation time options
    private double simulationDuration;
    private long uiDelayMs;

    // multiserver service points
    private int numMechanics;
    private int numWashers;

    // random seed for reproducibility
    private long baseRandomSeed;

    // distributions
    private DistributionOptions interArrival;
    private DistributionOptions receptionService;
    private DistributionOptions mechanicService;
    private DistributionOptions washService;

    // routing probabilities
    private double probNeedsMechanic;
    private double probNeedsWash;

    // conditional wash program probabilities
    private double probWashExterior;
    private double probWashInterior;
    private double probWashBoth;

    // per-server speed multipliers (in UI as "experience" etc.)
    private double[] mechanicSpeedFactors;
    private double[] washerSpeedFactors;


    // ---------- Constructors ----------

    public SimulationOptions() {}

    // ---------- Defaults ----------

    public static SimulationOptions defaults() {
        SimulationOptions options = new SimulationOptions();
        options.simulationDuration = 100000.0;
        options.uiDelayMs = 100;

        // Initialize server counts
        options.numMechanics = 1;
        options.numWashers = 1;

        // Initialize distributions with defaults
        options.interArrival = DistributionOptions.negExp(15.0);
        options.receptionService = DistributionOptions.negExp(10.0);
        options.mechanicService = DistributionOptions.negExp(30.0);
        options.washService = DistributionOptions.negExp(20.0);

        // Initialize routing probabilities
        options.probNeedsMechanic = 0.7;
        options.probNeedsWash = 0.5;
        options.probWashExterior = 0.5;
        options.probWashInterior = 0.3;
        options.probWashBoth = 0.2;

        // Initialize speed factors for servers
        options.mechanicSpeedFactors = new double[]{1.0};
        options.washerSpeedFactors = new double[]{1.0};

        // Initialize random seed
        options.baseRandomSeed = System.currentTimeMillis();


        return options;
    }

    private double[] makeSpeedArray(int n) {
        if (n <= 0) return new double[0];

        double[] defaultArr = new double[n];
        Arrays.fill(defaultArr, 1.0);
        return defaultArr;
    }

    private double[] ensureSize(double[] arr, int n) {
        if (n <= 0) return new double[0];
        if (arr == null || arr.length < n) {
            double[] newArr = new double[n];
            Arrays.fill(newArr, 1.0); // Fill with default speed factor
            if (arr != null) {
                // Copy existing values
                System.arraycopy(arr, 0, newArr, 0, Math.min(arr.length, n));
            }
            return newArr;
        }
        if (arr.length > n) {
            // Need to shrink array
            double[] newArr = new double[n];
            System.arraycopy(arr, 0, newArr, 0, n);
            return newArr;
        }
        return arr;
    }

    private void normalizeWashProgramProbs() {
        double sum = this.probWashExterior + this.probWashInterior + this.probWashBoth;
        if (sum <= 0) {
            this.probWashExterior = 0.5;
            this.probWashInterior /= 0.3;
            this.probWashBoth /= 0.2;
            return;
        }
        this.probWashExterior /= sum;
        this.probWashInterior /= sum;
        this.probWashBoth /= sum;
    }


    // ---------- Getters and Setters ----------
    public double getSimulationDuration() { return this.simulationDuration; }
    public void setSimulationDuration(double simulationDuration) { this.simulationDuration = simulationDuration; }

    public long getUiDelayMillis() { return this.uiDelayMs; }
    public void setUiDelayMillis(long uiDelayMillis) { this.uiDelayMs = uiDelayMillis; }

    public int getMechanicServers() { return this.numMechanics; }
    public void setMechanicServers(int mechanicServers) { this.numMechanics = mechanicServers; }

    public int getWashServers() { return this.numWashers; }
    public void setWashServers(int washServers) { this.numWashers = washServers; }

    public double getProbNeedsMechanic() { return probNeedsMechanic; }
    public void setProbNeedsMechanic(double probability) { this.probNeedsMechanic = probability; }

    public double getProbNeedsWash() { return probNeedsWash; }
    public void setProbNeedsWash(double probability) { this.probNeedsWash = probability; }

    public double getWashProbExterior() { return this.probWashExterior; }
    public void setWashProbExterior(double probability) { this.probWashExterior = probability; }

    public double getWashProbInterior() { return this.probWashInterior; }
    public void setWashProbInterior(double probability) { this.probWashInterior = probability; }

    public double getWashProbBoth() { return probWashBoth; }
    public void setWashProbBoth(double probability) { this.probWashBoth = probability; }

    public long getBaseRandomSeed() { return this.baseRandomSeed; }
    public void setBaseRandomSeed(long baseRandomSeed) { this.baseRandomSeed = baseRandomSeed; }

    public DistributionOptions getInterArrival() { return this.interArrival; }
    public void setInterArrival(DistributionOptions interArrival) { this.interArrival = interArrival; }

    public DistributionOptions getReceptionService() { return this.receptionService; }
    public void setReceptionService(DistributionOptions receptionService) { this.receptionService = receptionService; }

    public DistributionOptions getMechanicService() { return this.mechanicService; }
    public void setMechanicService(DistributionOptions mechanicService) { this.mechanicService = mechanicService; }

    public DistributionOptions getWashService() { return this.washService; }
    public void setWashService(DistributionOptions washService) { this.washService = washService; }

    // Safe accessors (always correct length)
    public double[] getMechanicSpeedFactors() {
        mechanicSpeedFactors = ensureSize(mechanicSpeedFactors, numMechanics);
        return mechanicSpeedFactors.clone();
    }

    public void setMechanicSpeedFactors(double[] factors) {
        mechanicSpeedFactors = ensureSize(factors, numMechanics);
    }

    public double[] getWashSpeedFactors() {
        washerSpeedFactors = ensureSize(washerSpeedFactors, numWashers);
        return washerSpeedFactors.clone();
    }

    public void setWashSpeedFactors(double[] factors) {
        washerSpeedFactors = ensureSize(factors, numWashers);
    }
}