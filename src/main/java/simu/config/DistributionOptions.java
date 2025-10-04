package simu.config;

import distributions.ContinuousGenerator;
import distributions.Negexp;
import distributions.Normal;
import distributions.Uniform;

public class DistributionOptions {
    public enum DistributionType {
        NEGEXP,
        NORMAL,
        UNIFORM
    }

    private DistributionType type;

    private double mean;
    private double stdDev; // for NORMAL
    private double min;    // for UNIFORM
    private double max;    // for UNIFORM

    private DistributionOptions(DistributionType type, double mean, double stdDev, double min, double max) {
        this.type = type;
        this.mean = mean;
        this.stdDev = stdDev;
        this.min = min;
        this.max = max;
    }


    // ---------- Factories ----------

    public static DistributionOptions negExp(double mean) {
        return new DistributionOptions(DistributionType.NEGEXP, mean, 0.0, 0.0, 0.0);
    }

    public static DistributionOptions normal(double mean, double stdDev) {
        return new DistributionOptions(DistributionType.NORMAL, mean, stdDev, 0.0, 0.0);
    }

    public static DistributionOptions uniform(double min, double max) {
        return new DistributionOptions(DistributionType.UNIFORM, 0.0, 0.0, min, max);
    }


    // ---------- Gen creation ----------

    public ContinuousGenerator toGen(long seed) {
        switch (this.type) {
            case NEGEXP: return new Negexp(this.mean, seed);
            case NORMAL: return new Normal(this.mean, this.stdDev, seed);
            case UNIFORM: return new Uniform(this.min, this.max, seed);
            default: throw new IllegalStateException("Unknown distribution type: " + this.type);
        }
    }


    // ---------- Getters ----------
    public DistributionType getType() {
        return this.type;
    }
    public double getMean() {
        return this.mean;
    }
    public double getStdDev() {
        return this.stdDev;
    }
    public double getMin() {
        return this.min;
    }
    public double getMax() {
        return this.max;
    }
}
