package simu.model;

import distributions.ContinuousGenerator;
import simu.config.SimulationOptions;
import simu.controller.IControllerMtoV;
import simu.framework.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EngineMod extends Engine {
    // single-server service points
    private final ServicePoint reception;

    // multiserver service points
    private final ServicePoint mechanic;
    private final ServicePoint wash;

    // add counters for different wash programs
    private int washExteriorCount;
    private int washInteriorCount;
    private int washBothCount;

    // arrival process
    private final ArrivalProcess arrivals;
    private final Random rng;

    // options (gathered from UI)
    // contains all the parameters for the simulation
    // like number of servers, distributions, probabilities, etc.
    private SimulationOptions options;

    // controller
    private IControllerMtoV controller;

    /**
     * Custom generator that modifies the speed of a base generator by a given factor.
     * For example, a speed factor of 2.0 makes the service twice as fast (halves the time).
     * This can be used to model servers with different efficiencies. (e.g. more experienced workers)
     */
    private static class CustomGen implements ContinuousGenerator {
        private ContinuousGenerator base;
        private double speedFactor;

        CustomGen(ContinuousGenerator base, double speedFactor) {
            this.base = base;
            this.speedFactor = speedFactor <= 0.0 ? 1.0 : speedFactor;
        }
        @Override
        public double sample() {
            return this.base.sample() / this.speedFactor;
        }
        @Override
        public void setSeed(long seed){}
        @Override
        public long getSeed(){ return this.base.getSeed(); }
        @Override
        public void reseed(){}
    }


    // ---------- Constructors ----------------
    public EngineMod() {
        this(SimulationOptions.defaults(), null);
    }

    public EngineMod(IControllerMtoV controller) {
        this(SimulationOptions.defaults(), controller);
    }

    public EngineMod(SimulationOptions options, IControllerMtoV controller) {
        this.options   = options;
        this.controller= controller;
        this.rng       = new Random(options.getBaseRandomSeed());
        this.reception = buildReception(options);
        this.mechanic  = buildMechanic(options);
        this.wash      = buildWash(options);
        this.arrivals  = buildArrivals(options);

        if (controller != null) {
            controller.updateServicePoints(options.getMechanicServers(), options.getWashServers());
        }
    }


    // ---------- Initialization ----------

    private ArrivalProcess buildArrivals(SimulationOptions options) {
        ContinuousGenerator gen = options.getInterArrival().toGen(this.nextSeed());
        return new ArrivalProcess(gen, this.eventList, EventType.ARRIVAL);
    }

    private ServicePoint buildReception(SimulationOptions options) {
        ContinuousGenerator gen = options.getReceptionService().toGen(this.nextSeed());
        return new ServicePoint(gen, this.eventList, EventType.RECEPTION_END);
    }

    private ServicePoint buildMechanic(SimulationOptions options) {
        int n = options.getMechanicServers();
        double[] speeds = options.getMechanicSpeedFactors();
        ContinuousGenerator[] gens = new ContinuousGenerator[n];

        for (int i = 0; i < n; i++) {
            ContinuousGenerator base = options.getMechanicService().toGen(this.nextSeed());
            gens[i] = new CustomGen(base, speeds[i]);
        }

        return new ServicePoint(gens, this.eventList, EventType.MECHANIC_END);
    }

    private ServicePoint buildWash(SimulationOptions options) {
        int n = options.getWashServers();
        double[] speeds = options.getWashSpeedFactors();
        ContinuousGenerator[] gens = new ContinuousGenerator[n];

        for (int i = 0; i < n; i++) {
            ContinuousGenerator base = options.getWashService().toGen(this.nextSeed());
            gens[i] = new CustomGen(base, speeds[i]);
        }

        // hard coded factors (exterior, interior, both)
        double exterior = 0.8;
        double interior = 1.0;
        double both     = 1.4;

        return new ServicePoint(gens, this.eventList, EventType.WASH_END,
            (customer, serverId, base) -> switch (customer.getWashProgram()) {
                case EXTERIOR -> base * exterior;
                case INTERIOR -> base * interior;
                case BOTH -> base * both;
                default -> base;
            }
        );
    }

    private long nextSeed() {
        return Integer.toUnsignedLong(this.rng.nextInt());
    }


    // ---------- Engine lifecycle ----------

    @Override
    protected void initialize() {
        this.arrivals.generateNextEvent();
    }

    @Override
    protected void runEvent(Event e) {
        // Possible customer flows:
        //
        // Arrival -> Reception -> Departure
        // Arrival -> Reception -> Mechanic -> Departure
        // Arrival -> Reception -> Wash -> Departure
        // Arrival -> Reception -> Mechanic -> Wash -> Departure

        double now = Clock.getInstance().getClock();

        switch ((EventType) e.getType()) {
            case ARRIVAL: {
                Customer c = new Customer();
                this.decideRouting(c);
                c.tReceptionQIn = now;
                this.reception.addQueue(c);

                if (this.controller != null) {
                    this.controller.visualiseCustomer();
                }

                this.arrivals.generateNextEvent();
                break;
            }
            case RECEPTION_END: {
                this.handleEnd(this.reception, EventType.RECEPTION_END, now);
                break;
            }
            case MECHANIC_END: {
                this.handleEnd(this.mechanic, EventType.MECHANIC_END, now);
                break;
            }
            case WASH_END: {
                this.handleEnd(this.wash, EventType.WASH_END, now);
                break;
            }
            default: break;
        }
    }

    /**
     * Decide the routing for the customer after reception
     * @param c Customer whose routing is to be decided
     */
    private void decideRouting(Customer c) {
        boolean mech = this.rng.nextDouble() < this.options.getProbNeedsMechanic();
        boolean wash = this.rng.nextDouble() < this.options.getProbNeedsWash();

        c.setNeedsMechanic(mech);
        c.setNeedsWash(wash);

        if (wash) {
            double p = this.rng.nextDouble();
            double ext = this.options.getWashProbExterior();
            double inter = ext + this.options.getWashProbInterior();

            if (p < ext) {
                c.setWashProgram(Customer.WashProgram.EXTERIOR);
                this.washExteriorCount++;
            } else if (p < inter) {
                c.setWashProgram(Customer.WashProgram.INTERIOR);
                this.washInteriorCount++;
            } else {
                c.setWashProgram(Customer.WashProgram.BOTH);
                this.washBothCount++;
            }
        }
    }

    /**
     * Handle the end of service at a service point.
     * This includes processing all customers who have finished service,
     * starting new services if possible, and updating the visualization.
     * @param sp Service point where the service has ended
     * @param type Event type corresponding to the service point
     * @param now Current simulation time
     */
    private void handleEnd(ServicePoint sp, EventType type, double now) {
        while (true) {
            ServicePoint.EndInfo ei = sp.finishService(now);
            if (ei == null) break;
            Customer c = ei.customer;
            switch (type) {
                case RECEPTION_END:
                    c.tReceptionEnd = now;
                    if (c.needsMechanic()) {
                        c.tMechanicQIn = now;
                        mechanic.addQueue(c);
                        if (controller != null) controller.visualiseCustomerToMechanic(c.getId(), mechanic.getAssignedServer(c));
                    } else if (c.needsWash()) {
                        c.tWashQIn = now;
                        wash.addQueue(c);
                        if (controller != null) controller.visualiseCustomerToWasher(c.getId(), wash.getAssignedServer(c));
                    } else {
                        c.tDeparture = now;
                        if (controller != null) controller.visualiseCustomerExit(c.getId());
                    }
                    break;
                case MECHANIC_END:
                    if (c.needsWash()) {
                        c.tWashQIn = now;
                        wash.addQueue(c);
                        if (controller != null) controller.visualiseCustomerToWasher(c.getId(), wash.getAssignedServer(c));
                    } else {
                        c.tDeparture = now;
                        if (controller != null) controller.visualiseCustomerExit(c.getId());
                    }
                    break;
                case WASH_END:
                    c.tDeparture = now;
                    if (controller != null) controller.visualiseCustomerExit(c.getId());
                    break;
                default: break;
            }
        }

        // Start as many new services as possible (multi-server safe)
        while (true) {
            ServicePoint.StartInfo si = sp.tryStart(now);
            if (si == null) break;
            switch (type) {
                case RECEPTION_END: si.customer.tReceptionStart = now;  break;
                case MECHANIC_END:  si.customer.tMechanicStart = now;   break;
                case WASH_END:      si.customer.tWashStart = now;       break;
                default: break;
            }
        }

        if (controller != null) {
            int receptionQueueLength = reception.getQueueLength();
            int[] mechanicQueues = mechanic.getQueueLengthsPerServer();
            int[] washerQueues = wash.getQueueLengthsPerServer();
            controller.updateQueueLengths(receptionQueueLength, mechanicQueues, washerQueues);
        }
    }

    @Override
    protected void tryCEvents() {
        double now = Clock.getInstance().getClock();

        // single-server service points
        this.startIfPossible(this.reception, EventType.RECEPTION_END, now);

        // possible multiserver service points
        this.startIfPossible(this.mechanic,  EventType.MECHANIC_END,  now);
        this.startIfPossible(this.wash,      EventType.WASH_END,      now);

        // Update queue lengths in visualization
        if (controller != null) {
            int receptionQueueLength = reception.getQueueLength();
            int[] mechanicQueues = mechanic.getQueueLengthsPerServer();
            int[] washerQueues = wash.getQueueLengthsPerServer();
            controller.updateQueueLengths(receptionQueueLength, mechanicQueues, washerQueues);
        }
    }

    @Override
    protected void results() {
        double now = Clock.getInstance().getClock();

        SimulationData data = SimulationData.from(now, this.reception, this.mechanic, this.wash);

        if (this.controller != null) {
            this.controller.simulationFinished(now, data);
        }

        System.out.println("\n--- Final statistics ---");
        this.printPoint("Reception", this.reception, now);
        this.printPointWithServers("Mechanic", this.mechanic, now);
        this.printPointWithServers("Wash", this.wash, now);
    }


    // ---------- Helper methods ----------

    /**
     * Try to start as many services as possible at the given service point.
     * @param sp Service point where to start services
     * @param type Event type to be used for the service end events
     * @param now Current simulation time
     */
    private void startIfPossible(ServicePoint sp, EventType type, double now) {
        int free = sp.availableSlots();

        if (free <= 0) return;

        for (int i = 0; i < free; i++) {
            ServicePoint.StartInfo si = sp.tryStart(now);

            if (si == null) break;

            switch (type) {
                case RECEPTION_END: si.customer.tReceptionStart = now;  break;
                case MECHANIC_END:  si.customer.tMechanicStart = now;   break;
                case WASH_END:      si.customer.tWashStart = now;       break;
                default: break;
            }
        }
    }


    // ---------- Printing helpers ----------
    // TODO: deprecate when ui is implemented
    private void printPoint(String label, ServicePoint sp, double now) {
        int cap = sp.getCapacity();
        int served = sp.getServedCount();
        double avgWait = sp.getAverageWaitTime();
        double avgService = sp.getAverageServiceTime();
        double avgTotal = avgWait + avgService;
        double util = (now > 0 && cap > 0) ? sp.getBusyTime() / (cap * now) : 0.0;

        System.out.printf("%s: servers=%d, served=%d, avgWait=%.3f, avgService=%.3f, avgTotal=%.3f, util=%.1f%%%n",
            label, cap, served, avgWait, avgService, avgTotal, util * 100.0);
    }
    private void printPointWithServers(String label, ServicePoint sp, double now) {
        this.printPoint(label, sp, now);
        double[] busy = sp.getPerServerBusyTimeSnapshot();
        int[] served = sp.getPerServerServedSnapshot();
        for (int i = 0; i < busy.length; i++) {
            double u = now > 0 ? busy[i] / now : 0.0;
            System.out.printf("  %s #%d: served=%d, busy=%.3f, util=%.1f%%%n",
                label, i + 1, served[i], busy[i], u * 100.0);
        }
    }
}
