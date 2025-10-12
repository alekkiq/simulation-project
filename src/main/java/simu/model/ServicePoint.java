package simu.model;

import distributions.ContinuousGenerator;
import simu.framework.*;
import java.util.LinkedList;

public class ServicePoint {

	// ---------- Nested types ----------

	/**
	 * Strategy interface for adjusting sampled service times.
	 * The strategy is called after sampling the base service time from the generator,
	 * but before scheduling the end event.
	 * This can be used to implement per-customer or per-server adjustments.
	 */
	@FunctionalInterface
	public interface ServiceTimeStrategy {
		double adjust(Customer c, int serverId, double baseSample);
	}

	/**
	 * Immutable DTO describing a service start decision.
	 * <p>
	 * Created by {@link #tryStart(double)} when a customer is dequeued and assigned to a free server.
	 * Consumed by the engine to annotate the customer and for optional logging.
	 * </p>
	 * <h4>Semantics</h4>
	 * <ul>
	 *   <li>{@code customer}: the customer entering service now.</li>
	 *   <li>{@code serviceTime}: sampled duration assigned to this service instance.</li>
	 *   <li>{@code endTime}: absolute time when the service will finish (and an end-event will fire).</li>
	 *   <li>{@code serverId}: zero-based id of the server that took the customer.</li>
	 * </ul>
	 */
	public static class StartInfo {
		public final Customer customer;
		public final double serviceTime;
		public final double endTime;
		public final int serverId;

		private StartInfo(Customer c, double serviceTime, double endTime, int serverId) {
			this.customer = c;
			this.serviceTime = serviceTime;
			this.endTime = endTime;
			this.serverId = serverId;
		}
	}

	/**
	 * Immutable DTO describing a completed service.
	 * <p>
	 * Created by {@link #finishService(double)} when the next finishing service is popped.
	 * Consumed by the engine to route the customer to the next stage and for logging.
	 * </p>
	 * <h4>Semantics</h4>
	 * <ul>
	 *   <li>{@code customer}: the customer whose service just finished.</li>
	 *   <li>{@code serverId}: zero-based id of the server that completed the service.</li>
	 *   <li>{@code startTime}: absolute time when this service started.</li>
	 *   <li>{@code endTime}: absolute time when this service finished.</li>
	 * </ul>
	 */
	public static class EndInfo {
		public final Customer customer;
		public final int serverId;
		public final double startTime;
		public final double endTime;

		private EndInfo(Customer customer, int serverId, double startTime, double endTime) {
			this.customer = customer;
			this.serverId = serverId;
			this.startTime = startTime;
			this.endTime = endTime;
		}
	}

	/**
	 * Internal queue node with arrival timestamp at this service point.
	 * - `enqueuedAt` is used to accumulate wait-time statistics.
	 * - Each server has its own FIFO queue of `QItem`s.
	 */
	private static class QItem {
		final Customer customer;
		final double enqueuedAt;

		QItem(Customer customer, double enqueuedAt) {
			this.customer = customer;
			this.enqueuedAt = enqueuedAt;
		}
	}

	// Config / infra
	private final EventList eventList;
	private final EventType endType;
	private final int capacity;
	private final ContinuousGenerator[] generators;
	private final ServiceTimeStrategy timeStrategy;

	// Queues (one per server)
	private final LinkedList<QItem>[] queues;

	// Active service state per server
	private final Customer[] active;
	private final double[] startTimes;
	private final double[] endTimes; // POSITIVE_INFINITY if idle

	// Stats
	private int served = 0;
	private double totalWaitTime = 0.0;
	private double totalBusyTime = 0.0;
	private final double[] perServerBusy;
	private final int[] perServerServed;
	private final double[] perServerWaitSum;


	// ---------- Constructors ----------

	public ServicePoint(ContinuousGenerator gen, EventList el, EventType type) {
		this(gen, el, type, 1);
	}

	public ServicePoint(ContinuousGenerator gen, EventList el, EventType type, int capacity) {
		this(gen, el, type, capacity, null);
	}

	/**
	 * Create the service point with a waiting queue.
	 */
	public ServicePoint(ContinuousGenerator gen, EventList el, EventType type, int capacity, ServiceTimeStrategy strategy) {
		this.capacity = capacity;
		this.eventList = el;
		this.endType = type;
		this.timeStrategy = strategy;
		this.generators = new ContinuousGenerator[this.capacity];

		for (int i = 0; i < this.capacity; i++)
			this.generators[i] = gen;

		this.queues = new LinkedList[this.capacity];

		for (int i = 0; i < this.capacity; i++)
			this.queues[i] = new LinkedList<>();

		this.active = new Customer[this.capacity];
		this.startTimes = new double[this.capacity];
		this.endTimes = new double[this.capacity];

		for (int i = 0; i < this.capacity; i++)
			endTimes[i] = Double.POSITIVE_INFINITY;

		this.perServerBusy = new double[this.capacity];
		this.perServerServed = new int[this.capacity];
		this.perServerWaitSum = new double[this.capacity];
	}

	public ServicePoint(ContinuousGenerator[] gens, EventList el, EventType type) {
		this(gens, el, type, null);
	}

	public ServicePoint(ContinuousGenerator[] gens, EventList el, EventType type, ServiceTimeStrategy strategy) {
		this.capacity = gens.length;
		this.eventList = el;
		this.endType = type;
		this.timeStrategy = strategy;
		this.generators = new ContinuousGenerator[this.capacity];

		System.arraycopy(gens, 0, this.generators, 0, this.capacity);
		this.queues = new LinkedList[this.capacity];

		for (int i = 0; i < this.capacity; i++)
			this.queues[i] = new LinkedList<>();

		this.active = new Customer[this.capacity];
		this.startTimes = new double[this.capacity];
		this.endTimes = new double[this.capacity];

		for (int i = 0; i < this.capacity; i++)
			endTimes[i] = Double.POSITIVE_INFINITY;

		this.perServerBusy = new double[this.capacity];
		this.perServerServed = new int[this.capacity];
		this.perServerWaitSum = new double[this.capacity];
	}


	// ---------- Queue operations ----------

	/**
	 * Add a customer to the queue of the server with the shortest estimated total processing time.
	 * This is calculated as (queue length * average service time + current service remaining time).
	 * @param c the customer to enqueue
	 */
	public void addQueue(Customer c) {
		double now = Clock.getInstance().getClock();
		int sid = this.selectShortestQueueServer();
		this.queues[sid].addLast(new QItem(c, now));
	}

	/**
	 * Select the server whose queue has the shortest estimated total processing time.
	 * This is calculated as (queue length * average service time + current service remaining time).
	 * @see #getAverageServiceTime()
	 * @return index of the selected server
	 */
	private int selectShortestQueueServer() {
		// Find server with shortest total time (queue length * average service time + current service remaining time)
		int best = 0;
		double bestTotalTime = Double.POSITIVE_INFINITY;
		double now = Clock.getInstance().getClock();

		for (int i = 0; i < this.capacity; i++) {
			// Calculate estimated total processing time for this server
			double queueTime = this.queues[i].size() * this.getAverageServiceTime();

			// Add remaining service time if server is busy
			double remainingTime = 0;
			if (this.active[i] != null) {
				remainingTime = Math.max(0, this.endTimes[i] - now);
			}

			double totalTime = queueTime + remainingTime;

			if (totalTime < bestTotalTime) {
				bestTotalTime = totalTime;
				best = i;
			}
		}
		return best;
	}

	/**
	 * Try to start a new service now.
	 * If a server is idle and has a waiting customer, the customer is dequeued,
	 * assigned to the server, and a service end event is scheduled.
	 * If no server is free or no customers are waiting, returns null.
	 * @param now current simulation time
	 * @return info about the started service, or null if none was started
	 */
	public StartInfo tryStart(double now) {
		// find an idle server that has a waiting customer
		for (int sid = 0; sid < this.capacity; sid++) {
			if (this.active[sid] == null && !this.queues[sid].isEmpty()) {
				QItem qi = this.queues[sid].removeFirst();
				Customer c = qi.customer;

				double wait = Math.max(0.0, now - qi.enqueuedAt);
				this.totalWaitTime += wait;
				this.perServerWaitSum[sid] += wait;

				double baseSample = this.generators[sid] != null ? this.generators[sid].sample() : 0.0;
				double serviceTime = (timeStrategy != null) ? timeStrategy.adjust(c, sid, baseSample) : baseSample;

				double end = now + serviceTime;
				if (end <= now) end = Math.nextUp(now); // simple safeguard

				this.active[sid] = c;
				this.startTimes[sid] = now;
				this.endTimes[sid] = end;

				this.eventList.add(new Event(this.endType, end));
				return new StartInfo(c, serviceTime, end, sid);
			}
		}
		return null;
	}


	// ---------- Finish logic ----------

	/**
	 * Finish the next service that is done at or before `now`.
	 * If multiple services finish at the same time, the one with the lowest server ID is chosen.
	 * If no service is done yet, returns null.
	 * @param now current simulation time
	 * @return info about the finished service, or null if none was finished
	 */
	public EndInfo finishService(double now) {
		int sid = this.findEarliestFinished(now);
		if (sid < 0) return null;

		Customer c = this.active[sid];
		double start = this.startTimes[sid];
		double end = this.endTimes[sid];

		this.active[sid] = null;
		this.endTimes[sid] = Double.POSITIVE_INFINITY;

		double busy = Math.max(0.0, end - start);
		this.totalBusyTime += busy;
		this.perServerBusy[sid] += busy;
		this.perServerServed[sid]++;

		this.served++;
		return new EndInfo(c, sid, start, end);
	}

	/**
	 * Find the server that has finished its service the earliest (at or before `now`).
	 * If multiple servers finished at the same time, the one with the lowest server ID is chosen.
	 * If no server has finished yet, returns -1.
	 * @param now current simulation time
	 * @return index of the server that finished earliest, or -1 if none
	 */
	private int findEarliestFinished(double now) {
		int best = -1;
		double bestEnd = Double.POSITIVE_INFINITY;
		final double EPS = 1e-9;

		for (int i = 0; i < this.capacity; i++) {
			if (this.active[i] != null && this.endTimes[i] - now <= EPS) {
				if (this.endTimes[i] < bestEnd) {
					bestEnd = this.endTimes[i];
					best = i;
				}
			}
		}

		return best;
	}


	// ---------- Info for engine ----------

	/**
	 * Check how many servers are idle and have a waiting customer.
	 * This is used by the engine to decide whether to try starting a new service.
	 * @return number of servers that can start a new service now
	 */
	public int availableSlots() {
		int free = 0;
		for (int i = 0; i < this.capacity; i++) {
			if (this.active[i] == null && !this.queues[i].isEmpty()) {
				free++;
			}
		}
		return free;
	}


	// ---------- Getters and analytics ----------

	public int getServedCount() { return this.served; }
	public int getCapacity() { return this.capacity; }
	public double getBusyTime() { return this.totalBusyTime; }
	public double getAverageServiceTime() { return this.served > 0 ? this.totalBusyTime / this.served : 0.0; }
	public double getAverageWaitTime() { return this.served > 0 ? this.totalWaitTime / this.served : 0.0; }
	public double getTotalWaitTime() { return this.totalWaitTime; }

	/**
	 * Get a snapshot of how much time each server has been busy.
	 * @return array of length `capacity` with per-server busy times
	 */
	public double[] getPerServerBusyTimeSnapshot() {
		double[] copy = new double[this.perServerBusy.length];
		System.arraycopy(this.perServerBusy, 0, copy, 0, copy.length);
		return copy;
	}

	/**
	 * Get a snapshot of how many customers each server has served.
	 * @return array of length `capacity` with per-server served counts
	 */
	public int[] getPerServerServedSnapshot() {
		int[] copy = new int[this.perServerServed.length];
		System.arraycopy(this.perServerServed, 0, copy, 0, copy.length);
		return copy;
	}

    /**
     * Get the server ID that is currently serving or will serve this customer
     */
    public int getAssignedServer(Customer customer) {
        // Check active servers first
        for (int i = 0; i < capacity; i++) {
            if (active[i] == customer) {
                return i;
            }
        }

        // Check queues
        for (int i = 0; i < capacity; i++) {
            for (QItem item : queues[i]) {
                if (item.customer == customer) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Get number of customers in each server's queue
     */
    public int[] getQueueLengthsPerServer() {
        int[] lengths = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            lengths[i] = queues[i].size() + (active[i] != null ? 1 : 0);
        }
        return lengths;
    }

	/**
	 * Get per-server average wait times
	 * @return array of length `capacity` with per-server average wait times
	 */
	public double[] getPerServerAverageWaitTimes() {
		double[] avg = new double[this.capacity];
		for (int i = 0; i < this.capacity; i++) {
			avg[i] = perServerServed[i] > 0 ? perServerWaitSum[i] / perServerServed[i] : 0.0;
		}
		return avg;
	}

	/**
	 * Get per-server average service times
	 * @return array of length `capacity` with per-server average service times
	 */
	public double[] getPerServerAverageServiceTimes() {
		double[] avg = new double[this.capacity];
		for (int i = 0; i < this.capacity; i++) {
			avg[i] = perServerServed[i] > 0 ? perServerBusy[i] / perServerServed[i] : 0.0;
		}
		return avg;
	}

	/**
	 * Get per-server average total times (wait + service)
	 * @return array of length `capacity` with per-server average total times
	 */
	public double[] getPerServerAverageTotalTimes() {
		double[] wait = getPerServerAverageWaitTimes();
		double[] svc = getPerServerAverageServiceTimes();
		double[] tot = new double[this.capacity];
		for (int i = 0; i < this.capacity; i++) {
			tot[i] = wait[i] + svc[i];
		}
		return tot;
	}

    /**
     * Get total number of customers at this service point
     */
    public int getQueueLength() {
        int total = 0;
        for (LinkedList<QItem> queue : queues) {
            total += queue.size();
        }
        for (Customer c : active) {
            if (c != null) total++;
        }
        return total;
    }
}
