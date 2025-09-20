package simu.model;

import eduni.distributions.ContinuousGenerator;
import eduni.distributions.Negexp;
import eduni.distributions.Normal;
import eduni.distributions.Uniform;
import simu.framework.*;

import java.util.Random;

public class EngineMod extends Engine {
    // single-server service points
    private final ServicePoint reception;

    // multi-server service points
    private final ServicePoint mechanic;
    private final ServicePoint wash;

    // generators
    private final ContinuousGenerator interArrivalGen;
    private final Random rng;

    public EngineMod() {
        this(1, 1); // amount of servers for mechanic and wash
    }

    public EngineMod(int mechanics, int washers) {
        this.rng = new Random();

        // arrival process
        this.interArrivalGen = new Negexp(8.0, rng.nextLong());

        // service times
        // TODO: parameterization
        ContinuousGenerator recGen  = new Uniform(3.0, 7.0, this.rng.nextLong() | 1L);
        ContinuousGenerator mechGen = new Normal(30.0, 10.0, this.rng.nextLong() | 1L);
        ContinuousGenerator washGen = new Normal(15.0, 5.0, this.rng.nextLong() | 1L);

        // service points
        this.reception = new ServicePoint(recGen,  eventList, EventType.RECEPTION_END);
        this.mechanic  = new ServicePoint(mechGen, eventList, EventType.MECHANIC_END, mechanics);
        this.wash      = new ServicePoint(washGen, eventList, EventType.WASH_END, washers);
    }

    @Override
    protected void initialize() {
        double now = Clock.getInstance().getClock();
        scheduleNextArrival(now);
    }

    @Override
    protected void runEvent(Event e) {
        double now = Clock.getInstance().getClock();

        switch ((EventType) e.getType()) {
            case ARRIVAL: {
                Customer c = new Customer();
                decideRouting(c);

                // queue to reception
                c.tReceptionQIn = now;
                this.reception.addQueue(c);

                // schedule next arrival
                scheduleNextArrival(now);
                break;
            }

            case RECEPTION_END: {
                Customer c = this.reception.finishService(now);
                if (c == null) break;
                c.tReceptionEnd = now;

                // route next step
                if (c.needsMechanic()) {
                    c.tMechanicQIn = now;
                    this.mechanic.addQueue(c);
                } else if (c.needsWash()) {
                    c.tWashQIn = now;
                    this.wash.addQueue(c);
                } else {
                    depart(c, now);
                }

                break;
            }

            case MECHANIC_END: {
                Customer c = this.mechanic.finishService(now);
                if (c == null) break;
                c.tMechanicEnd = now;

                if (c.needsWash() && c.tWashEnd <= 0) {
                    c.tWashQIn = now;
                    this.wash.addQueue(c);
                } else {
                    depart(c, now);
                }

                break;
            }

            case WASH_END: {
                Customer c = this.wash.finishService(now);
                if (c == null) break;
                c.tWashEnd = now;

                if (c.needsMechanic() && c.tMechanicEnd <= 0) {
                    c.tMechanicQIn = now;
                    this.mechanic.addQueue(c);
                } else {
                    depart(c, now);
                }

                break;
            }

            // departure handled in-line
            default: break;
        }
    }

    @Override
    // TODO add some printing
    protected void tryCEvents() {
        double now = Clock.getInstance().getClock();

        // single-server service points
        startIfPossible(this.reception, EventType.RECEPTION_END,    now);

        // multi-server service points
        startIfPossible(this.mechanic,  EventType.MECHANIC_END,     now);
        startIfPossible(this.wash,      EventType.WASH_END,         now);
    }

    @Override
    protected void results() {
        double now = Clock.getInstance().getClock();
        System.out.println("Simulation ended at " + now);
        System.out.println("Reception served=" + this.reception.getServedCount() + ", busy=" + this.reception.getBusyTime());
        System.out.println("Mechanic  served=" + this.mechanic.getServedCount()  + ", busy=" + this.mechanic.getBusyTime());
        System.out.println("Wash      served=" + this.wash.getServedCount()      + ", busy=" + this.wash.getBusyTime());
    }

    private static double safeFutureTime(double candidate, double now) {
        if (!Double.isFinite(candidate) || candidate <= now)
            return Math.nextUp(now);
        return candidate;
    }

    private void scheduleNextArrival(double now) {
        double dt = this.interArrivalGen.sample();

        if (!Double.isFinite(dt) || dt <= 0.0) {
            Trace.out(Trace.Level.WAR,
                    "Refusing to schedule arrival at " + dt + "; fixing to now+eps");
            dt = Math.ulp(now);
            if (dt <= 0.0) dt = 1e-6;
        }

        double at = safeFutureTime(now + dt, now);
        eventList.add(new Event(EventType.ARRIVAL, at));
        Trace.out(Trace.Level.INFO, "Adding to the event list ARRIVAL " + at);
    }

    private void startIfPossible(ServicePoint sp, EventType endType, double now) {
        int maxStarts = Math.max(1, sp.queueSize() + sp.getCapacity());
        int started = 0;

        while (started < maxStarts) {
            ServicePoint.StartInfo si = sp.tryStart(now);
            if (si == null) break;

            switch (endType) {
                case RECEPTION_END: si.customer.tReceptionStart = now;  break;
                case MECHANIC_END:  si.customer.tMechanicStart = now;   break;
                case WASH_END:      si.customer.tWashStart = now;       break;
                default: break;
            }

            System.out.printf("%s start -> customer=%d, service=%.3f, end=%.3f%n",
                    labelFor(endType), si.customer.getId(), si.serviceTime, si.endTime);

            double end = si.endTime;
            if (!Double.isFinite(end) || end < now) {
                Trace.out(Trace.Level.ERR, "Refusing to schedule " + endType + " at " + end + "; fixing to now+eps");
                end = now + 1e-6;
            }

            this.eventList.add(new Event(endType, end));
            started++;
        }

        if (started >= maxStarts) {
            Trace.out(Trace.Level.ERR,
            "Aborted start loop for " + endType + " at t=" + now
                + " (started=" + started + ", guard=" + maxStarts + "). Suspected livelock prevented.");
        }
    }

    private String labelFor(EventType et) {
        switch (et) {
            case RECEPTION_END: return "Reception";
            case MECHANIC_END:  return "Mechanic";
            case WASH_END:      return "Wash";
            case ARRIVAL:       return "Arrival";
            default:            return et.name();
        }
    }

    private void depart(Customer c, double now) {
        c.tDeparture = now;
        c.setRemovalTime(now);
        c.reportResults();
    }

    /**
     * Decide the routing for the customer
     * TODO: implement actual logic
     * @param c
     */
    private void decideRouting(Customer c) {
        boolean mech = rng.nextDouble() < 0.7;
        boolean wash = rng.nextDouble() < 0.5;

        c.setNeedsMechanic(mech);
        c.setNeedsWash(wash);

        // decide wash type
        // TODO: parameterization
        if (wash) {
            double p = rng.nextDouble();
            if (p < 0.4) c.setWashProgram(Customer.WashProgram.EXTERIOR);
            else if (p < 0.8) c.setWashProgram(Customer.WashProgram.INTERIOR);
            else c.setWashProgram(Customer.WashProgram.BOTH);
        }
    }
}
