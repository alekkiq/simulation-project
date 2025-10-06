package simu.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import simu.config.DistributionOptions;
import simu.config.SimulationOptions;

public final class SimParameters {
    // --- Core ---
    private final DoubleProperty  simDuration  = new SimpleDoubleProperty(1_000.0);
    private final IntegerProperty uiDelayMs    = new SimpleIntegerProperty(200);
    private final IntegerProperty numMechanics = new SimpleIntegerProperty(2);
    private final IntegerProperty numWashers   = new SimpleIntegerProperty(1);

    // --- Distributions ---
    private final ObjectProperty<DistributionOptions> interArrival =
            new SimpleObjectProperty<>(DistributionOptions.negExp(10.0));
    private final ObjectProperty<DistributionOptions> receptionService =
            new SimpleObjectProperty<>(DistributionOptions.uniform(3.0, 7.0));
    private final ObjectProperty<DistributionOptions> mechanicService =
            new SimpleObjectProperty<>(DistributionOptions.normal(30.0, 10.0));
    private final ObjectProperty<DistributionOptions> washService =
            new SimpleObjectProperty<>(DistributionOptions.normal(15.0, 5.0));

    // --- Probabilities ---
    private final DoubleProperty pNeedsMechanic = new SimpleDoubleProperty(0.70);
    private final DoubleProperty pNeedsWash     = new SimpleDoubleProperty(0.50);
    private final DoubleProperty pWashExterior  = new SimpleDoubleProperty(0.50);
    private final DoubleProperty pWashInterior  = new SimpleDoubleProperty(0.30);
    private final DoubleProperty pWashBoth      = new SimpleDoubleProperty(0.20);

    // --- Per-server speed factors (bindable, one DoubleProperty per server) ---
    private final ObservableList<DoubleProperty> mechanicSpeeds = FXCollections.observableArrayList();
    private final ObservableList<DoubleProperty> washerSpeeds   = FXCollections.observableArrayList();

    public SimParameters() {
        // Keep lists sized to counts (default factor = 1.0)
        numMechanics.addListener((o, a, b) -> ensureSize(mechanicSpeeds, safeSize(b), 1.0));
        numWashers.addListener((o, a, b)   -> ensureSize(washerSpeeds,   safeSize(b), 1.0));

        // Initialize lists to match initial counts
        ensureSize(mechanicSpeeds, numMechanics.get(), 1.0);
        ensureSize(washerSpeeds,   numWashers.get(),   1.0);
    }

    private static int safeSize(Number n) {
        int v = n == null ? 0 : n.intValue();
        return Math.max(0, v);
    }

    /** Grow/shrink list to 'size', filling new entries with properties at 'def' */
    private static void ensureSize(ObservableList<DoubleProperty> list, int size, double def) {
        // grow
        while (list.size() < size) list.add(new SimpleDoubleProperty(def));
        // shrink (drops trailing properties; GUI should rebuild its sliders)
        while (list.size() > size) list.remove(list.size() - 1);
    }

    public SimulationOptions toConfig() {
        SimulationOptions o = new SimulationOptions();
        o.setSimulationDuration(getSimDuration());
        o.setUiDelayMillis(getUiDelayMs());

        o.setMechanicServers(getNumMechanics());
        o.setWashServers(getNumWashers());

        o.setInterArrival(getInterArrival());
        o.setReceptionService(getReceptionService());
        o.setMechanicService(getMechanicService());
        o.setWashService(getWashService());

        o.setProbNeedsMechanic(pNeedsMechanicProperty().get());
        o.setProbNeedsWash(pNeedsWashProperty().get());

        o.setWashProbExterior(pWashExteriorProperty().get());
        o.setWashProbInterior(pWashInteriorProperty().get());
        o.setWashProbBoth(pWashBothProperty().get());

        o.setMechanicSpeedFactors(mechanicSpeedFactorsArray());
        o.setWashSpeedFactors(washerSpeedFactorsArray());

        // Optional: provide a reproducible seed if you add one to SimParameters later
        // o.setBaseRandomSeed(System.currentTimeMillis());

        return o;
    }

    // --- Property accessors ---
    public DoubleProperty  simDurationProperty()   { return simDuration; }
    public IntegerProperty uiDelayMsProperty()     { return uiDelayMs; }
    public IntegerProperty numMechanicsProperty()  { return numMechanics; }
    public IntegerProperty numWashersProperty()    { return numWashers; }

    public ObjectProperty<DistributionOptions> interArrivalProperty()     { return interArrival; }
    public ObjectProperty<DistributionOptions> receptionServiceProperty() { return receptionService; }
    public ObjectProperty<DistributionOptions> mechanicServiceProperty()  { return mechanicService; }
    public ObjectProperty<DistributionOptions> washServiceProperty()      { return washService; }

    public DoubleProperty pNeedsMechanicProperty() { return pNeedsMechanic; }
    public DoubleProperty pNeedsWashProperty()     { return pNeedsWash; }
    public DoubleProperty pWashExteriorProperty()  { return pWashExterior; }
    public DoubleProperty pWashInteriorProperty()  { return pWashInterior; }
    public DoubleProperty pWashBothProperty()      { return pWashBoth; }

    public ObservableList<DoubleProperty> mechanicSpeeds() { return mechanicSpeeds; }
    public ObservableList<DoubleProperty> washerSpeeds()   { return washerSpeeds; }

    // --- Convenience typed getters/setters (optional) ---
    public double getSimDuration()            { return simDuration.get(); }
    public void   setSimDuration(double v)    { simDuration.set(v); }
    public int    getUiDelayMs()              { return uiDelayMs.get(); }
    public void   setUiDelayMs(int v)         { uiDelayMs.set(v); }
    public int    getNumMechanics()           { return numMechanics.get(); }
    public void   setNumMechanics(int v)      { numMechanics.set(v); } // triggers ensureSize via listener
    public int    getNumWashers()             { return numWashers.get(); }
    public void   setNumWashers(int v)        { numWashers.set(v); }   // triggers ensureSize via listener

    public DistributionOptions getInterArrival()      { return interArrival.get(); }
    public void setInterArrival(DistributionOptions d){ interArrival.set(d); }
    public DistributionOptions getReceptionService()  { return receptionService.get(); }
    public void setReceptionService(DistributionOptions d){ receptionService.set(d); }
    public DistributionOptions getMechanicService()   { return mechanicService.get(); }
    public void setMechanicService(DistributionOptions d){ mechanicService.set(d); }
    public DistributionOptions getWashService()       { return washService.get(); }
    public void setWashService(DistributionOptions d) { washService.set(d); }

    // --- Snapshots for the engine ---
    public double[] mechanicSpeedFactorsArray() {
        return mechanicSpeeds.stream().mapToDouble(DoubleProperty::get).toArray();
    }
    public double[] washerSpeedFactorsArray() {
        return washerSpeeds.stream().mapToDouble(DoubleProperty::get).toArray();
    }

    @Override
    public String toString() {
        return "SimParameters{" +
                "simDuration=" + simDuration.get() +
                ", uiDelayMs=" + uiDelayMs.get() +
                ", numMechanics=" + numMechanics.get() +
                ", numWashers=" + numWashers.get() +
                ", interArrival=" + interArrival.get() +
                ", receptionService=" + receptionService.get() +
                ", mechanicService=" + mechanicService.get() +
                ", washService=" + washService.get() +
                ", pNeedsMechanic=" + pNeedsMechanic.get() +
                ", pNeedsWash=" + pNeedsWash.get() +
                ", pWashExterior=" + pWashExterior.get() +
                ", pWashInterior=" + pWashInterior.get() +
                ", pWashBoth=" + pWashBoth.get() +
                ", mechanicSpeeds=" + mechanicSpeeds +
                ", washerSpeeds=" + washerSpeeds +
                '}';
    }
}
