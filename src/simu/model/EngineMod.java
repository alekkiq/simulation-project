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

    // multiserver service points
    private final ServicePoint mechanic;
    private final ServicePoint wash;

    // arrival process
    private final ArrivalProcess arrivals;
    private final Random rng;

    // ---------- Constructors ----------------

    public EngineMod() {
        this(1, 1); // default amount of servers for mechanic and wash
    }

    public EngineMod(int mechanics, int washers) {
        this.rng = new Random();

       ContinuousGenerator interArrivalGen = new Negexp(10, Integer.toUnsignedLong(this.rng.nextInt()));
       this.arrivals = new ArrivalProcess(interArrivalGen, this.eventList, EventType.ARRIVAL);

        // TODO: parameterization to distributions
        ContinuousGenerator recGen  = new Uniform(3.0, 7.0, Integer.toUnsignedLong(this.rng.nextInt()));
        ContinuousGenerator mechGen = new Normal(30.0, 10.0, Integer.toUnsignedLong(this.rng.nextInt()));
        ContinuousGenerator washGen = new Normal(15.0, 5.0, Integer.toUnsignedLong(this.rng.nextInt()));

        // service points
        this.reception = new ServicePoint(recGen,  this.eventList, EventType.RECEPTION_END);
        this.mechanic  = new ServicePoint(mechGen, this.eventList, EventType.MECHANIC_END, mechanics);
        this.wash      = new ServicePoint(washGen, this.eventList, EventType.WASH_END, washers);
    }

    // ---------- Engine lifecycle ----------

    @Override
    protected void initialize() {
        arrivals.generateNextEvent();
    }

    @Override
    protected void runEvent(Event e) {
        double now = Clock.getInstance().getClock();

        /*
         * Possible customer flows:
         *
         * Arrival -> Reception -> Departure
         * Arrival -> Reception -> Mechanic -> Departure
         * Arrival -> Reception -> Wash -> Departure
         * Arrival -> Reception -> Mechanic -> Wash -> Departure
         */

        switch ((EventType) e.getType()) {
            case ARRIVAL: {
                Customer c = new Customer();
                decideRouting(c);
                c.tReceptionQIn = now;
                this.reception.addQueue(c);
                this.arrivals.generateNextEvent();
                break;
            }
            case RECEPTION_END: {
                int finished = 0;
                while (true) {
                    ServicePoint.EndInfo ei = this.reception.finishService(now);
                    if (ei == null) break;
                    finished++;

                    Customer c = ei.customer;
                    c.tReceptionEnd = now;
                    System.out.printf("Reception#%d end -> customer=%d at %.3f%n",
                        ei.serverId, c.getId(), now);

                    if (c.needsMechanic()) {
                        c.tMechanicQIn = now;
                        this.mechanic.addQueue(c);
                    } else if (c.needsWash()) {
                        c.tWashQIn = now;
                        this.wash.addQueue(c);
                    } else {
                        depart(c, now);
                    }
                }
                if (finished == 0) {
                    double tnext = this.reception.nextEndTime();
                    if (Double.isFinite(tnext)) {
                        tnext = safeFutureTime(tnext, now);
                        this.eventList.add(new Event(EventType.RECEPTION_END, tnext));
                    }
                }
                break;
            }
            case MECHANIC_END: {
                int finished = 0;
                while (true) {
                    ServicePoint.EndInfo ei = this.mechanic.finishService(now);
                    if (ei == null) break;
                    finished++;

                    Customer c = ei.customer;
                    c.tMechanicEnd = now;
                    System.out.printf("Mechanic#%d end -> customer=%d at %.3f%n",
                            ei.serverId, c.getId(), now);

                    if (c.needsWash() && c.tWashEnd <= 0) {
                        c.tWashQIn = now;
                        this.wash.addQueue(c);
                    } else {
                        depart(c, now);
                    }
                }
                if (finished == 0) {
                    double tnext = this.mechanic.nextEndTime();
                    if (Double.isFinite(tnext)) {
                        tnext = safeFutureTime(tnext, now);
                        this.eventList.add(new Event(EventType.MECHANIC_END, tnext));
                    }
                }
                break;
            }
            case WASH_END: {
                int finished = 0;
                while (true) {
                    ServicePoint.EndInfo ei = this.wash.finishService(now);
                    if (ei == null) break;
                    finished++;

                    Customer c = ei.customer;
                    c.tWashEnd = now;
                    System.out.printf("Wash#%d end -> customer=%d at %.3f%n",
                        ei.serverId, c.getId(), now);

                    // no chance for mechanic after wash
                    depart(c, now);
                }
                if (finished == 0) {
                    double tnext = this.wash.nextEndTime();
                    if (Double.isFinite(tnext)) {
                        tnext = safeFutureTime(tnext, now);
                        this.eventList.add(new Event(EventType.WASH_END, tnext));
                    }
                }
                break;
            }
            default: break;
        }
    }

    @Override
    protected void tryCEvents() {
        double now = Clock.getInstance().getClock();

        // single-server service points
        startIfPossible(this.reception, EventType.RECEPTION_END, now);

        // possible multiserver service points
        startIfPossible(this.mechanic,  EventType.MECHANIC_END,  now);
        startIfPossible(this.wash,      EventType.WASH_END,      now);
    }

    @Override
    protected void results() {
        double now = Clock.getInstance().getClock();

        System.out.println("\n--- Final statistics ---");
        printPoint("Reception", this.reception, now);
        printPointWithServers("Mechanic", this.mechanic, now);
        printPointWithServers("Wash", this.wash, now);
    }


    // ---------- Helper methods ----------

    /**
     * Try to start as many services as possible at the given service point.
     * @remarks This may start multiple services if there are multiple free servers.
     *          It may also start none if there is no queue or no free servers.
     * @param sp Service point where to start services
     * @param endType Event type to be used for the service end events
     * @param now Current simulation time
     */
    private void startIfPossible(ServicePoint sp, EventType endType, double now) {
        int free = sp.availableSlots();
        if (free <= 0) return;

        for (int i = 0; i < free; i++) {
            ServicePoint.StartInfo si = sp.tryStart(now);
            if (si == null) break;

            switch (endType) {
                case RECEPTION_END: si.customer.tReceptionStart = now;  break;
                case MECHANIC_END:  si.customer.tMechanicStart = now;   break;
                case WASH_END:      si.customer.tWashStart = now;       break;
                default: break;
            }

            System.out.printf("%s#%d start -> customer=%d, service=%.3f, end=%.3f%n",
                labelFor(endType), si.serverId, si.customer.getId(), si.serviceTime, si.endTime);
        }
    }

    /**
     * Process the departure of the customer from the system
     * @param c Customer who is departing
     * @param now Current simulation time
     */
    private void depart(Customer c, double now) {
        c.tDeparture = now;
        c.setRemovalTime(now);
        c.reportResults();
    }

    /**
     * Decide the routing for the customer after reception
     * @param c Customer whose routing is to be decided
     *
     * TODO: parameterize the probabilities
     */
    private void decideRouting(Customer c) {
        boolean mech = rng.nextDouble() < 0.7;
        boolean wash = rng.nextDouble() < 0.5;

        c.setNeedsMechanic(mech);
        c.setNeedsWash(wash);

        // decide wash type
        if (wash) {
            double p = rng.nextDouble();
            if (p < 0.4) c.setWashProgram(Customer.WashProgram.EXTERIOR);
            else if (p < 0.8) c.setWashProgram(Customer.WashProgram.INTERIOR);
            else c.setWashProgram(Customer.WashProgram.BOTH);
        }
    }

    private static double safeFutureTime(double candidate, double now) {
        if (!Double.isFinite(candidate) || candidate <= now)
            return Math.nextUp(now);
        return candidate;
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
        printPoint(label, sp, now);
        double[] busy = sp.getPerServerBusyTimeSnapshot();
        int[] served = sp.getPerServerServedSnapshot();
        for (int i = 0; i < busy.length; i++) {
            double u = now > 0 ? busy[i] / now : 0.0;
            System.out.printf("  %s #%d: served=%d, busy=%.3f, util=%.1f%%%n",
                label, i + 1, served[i], busy[i], u * 100.0);
        }
    }
}
