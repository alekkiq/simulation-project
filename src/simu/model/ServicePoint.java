package simu.model;

import eduni.distributions.ContinuousGenerator;
import simu.framework.*;

// import java.time.Clock;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * Service Point implements the functionalities, calculations and reporting.
 *
 * TODO: This must be modified to actual implementation. Things to be added:
 *     - functionalities of the service point
 *     - measurement variables added
 *     - getters to obtain measurement values
 *
 * Service point has a queue where customers are waiting to be serviced.
 * Service point simulated the servicing time using the given random number generator which
 * generated the given event (customer serviced) for that time.
 *
 * Service point collects measurement parameters.
 */
public class ServicePoint {
	public static class StartInfo {
		public final Customer customer;
		public final double serviceTime;
		public final double endTime;

		private StartInfo(Customer c, double serviceTime, double endTime) {
			this.customer = c;
			this.serviceTime = serviceTime;
			this.endTime = endTime;
		}
	}

	private static class InService implements Comparable<InService> {
		final Customer customer;
		final double startTime;
		final double endTime;

		InService(Customer customer, double startTime, double endTime) {
			this.customer = customer;
			this.startTime = startTime;
			this.endTime = endTime;
		}
		@Override
		public int compareTo(InService o) {
			return Double.compare(this.endTime, o.endTime);
		}
	}

	private LinkedList<Customer> queue = new LinkedList<>(); // Data Structure used
	private ContinuousGenerator generator;
	private EventList eventList;
	private EventType eventTypeScheduled;

	private final int capacity;
	private final PriorityQueue<InService> running = new PriorityQueue<>();

	// stats
	private int busy = 0;
	private double lastBusyChange = 0.0;
	private int served = 0;
	private double busyTime = 0.0;
	private double lastStart = 0.0;


	public ServicePoint(ContinuousGenerator gen, EventList eventList, EventType type) {
		this(gen, eventList, type, 1);
	}
	/**
	 * Create the service point with a waiting queue.
	 *
	 * @param generator Random number generator for service time simulation
	 * @param eventList Simulator event list, needed for the insertion of service ready event
	 * @param type Event type for the service end event
	 */
	public ServicePoint(ContinuousGenerator generator, EventList eventList, EventType type, int capacity) {
		this.eventList = eventList;
		this.generator = generator;
		this.eventTypeScheduled = type;
		this.capacity = Math.max(1, capacity);
		this.lastBusyChange = 0.0;
	}

	/**
	 * Try to start a new service, if the service point is not busy and there is customers on the queue
	 * @param now
	 * @return StartInfo if a new service was started, null otherwise
	 */
	public synchronized StartInfo tryStart(double now) {
		if (this.busy >= this.capacity) return null;
		if (this.queue.isEmpty()) return null;

		// reserve capacity and dequeue BEFORE sampling
		this.busy++;
		Customer c = this.queue.removeFirst();

		double st = this.generator.sample();
		if (!Double.isFinite(st) || st <= 0.0) {
			Trace.out(Trace.Level.WAR,
					"Invalid service time " + st + " from " + this.generator.getClass().getSimpleName()
							+ " at " + now + "; clamping to epsilon.");
			st = Math.max(Math.ulp(now), 1e-6);
		}

		double end = now + st;
		if (!Double.isFinite(end) || end <= now) {
			end = Math.nextUp(now);
			st = end - now;
		}

		// record running service so finishService can find it
		this.running.add(new InService(c, now, end));

		return new StartInfo(c, st, end);
	}

	/**
	 * Begins a new service, customer is on the queue during the service
	 * Inserts a new event to the event list when the service should be ready.
	 */
	@Deprecated
	public synchronized void beginService() {		// Begins a new service, customer is on the queue during the service
		double now = Clock.getInstance().getClock();
		StartInfo si = tryStart(now);
		if (si == null) return;

		Trace.out(Trace.Level.INFO,
				"Starting a new service for the customer #" + si.customer.getId() +
						" (service=" + si.serviceTime + ", end=" + si.endTime + ")");
		eventList.add(new Event(this.eventTypeScheduled, si.endTime)); // schedule once
	}

	/**
	 * Finish the current service, customer is removed from the service point
	 * @param now
	 * @return Customer who has been serviced, null if no customer was in service
	 */
	public synchronized Customer finishService(double now) {
		InService top = this.running.peek();
		if (top == null || (top.endTime - now) > 1e-9) {
			return null;
		}

		this.running.poll();
		if (this.busy > 0) this.busy--; // release capacity

		this.served++;
		// accumulate actual service time
		this.busyTime += Math.max(0.0, top.endTime - top.startTime);
		return top.customer;
	}

	/**
	 * Add a customer to the service point queue.
	 *
	 * @param a Customer to be queued
	 */
	public synchronized void addQueue(Customer a) {	// The first customer of the queue is always in service
		this.queue.addLast(a);
	}

	/**
	 * Remove customer from the waiting queue.
	 * Here we calculate also the appropriate measurement values.
	 *
	 * @return Customer retrieved from the waiting queue
	 */
	public Customer removeQueue() {
		return this.queue.poll();
	}

	/**
	 * Check whether the service point is currently reserved (busy)
	 * @return logical value indicating reservation status
	 */
	public synchronized boolean isReserved() {
		return !this.running.isEmpty();
	}

	/**
	 * Check whether there is customers on the waiting queue
	 *
	 * @return logival value indicating queue status
	 */
	public synchronized boolean isOnQueue() {
		return this.queue.size() != 0;
	}

	/** Get served customer count
	 * @return served customer count
	 */
	public int getServedCount() {
		return this.served;
	}

	/** Get the total busy time of the service point
	 * @return total busy time of the service point
	 */
	public double getBusyTime() {
		return this.busyTime;
	}

	/** Get the capacity of the service point (number of parallel services)
	 * @return capacity of the service point
	 */
	public int getCapacity() {
		return this.capacity;
	}

	/** Get the current queue size
	 * @return current queue size
	 */
	public int queueSize() {
		return this.queue.size();
	}
}
