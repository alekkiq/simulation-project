package simu.model;

import eduni.distributions.ContinuousGenerator;
import simu.framework.*;

// import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
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

	// ---------- Nested types ----------

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

	private static class InService implements Comparable<InService> {
		final Customer customer;
		final double startTime;
		final double endTime;
		final int serverId;

		InService(Customer customer, double startTime, double endTime, int serverId) {
			this.customer = customer;
			this.startTime = startTime;
			this.endTime = endTime;
			this.serverId = serverId;
		}
		@Override
		public int compareTo(InService o) {
			return Double.compare(this.endTime, o.endTime);
		}
	}

	private static class QItem {
		final Customer customer;
		final double enqueuedAt;

		QItem(Customer customer, double enqueuedAt) {
			this.customer = customer;
			this.enqueuedAt = enqueuedAt;
		}
	}

	// per server queues and generators
	private LinkedList<QItem>[] queues;
	private ContinuousGenerator[] generators;

	// event list and type
	private EventList eventList;
	private EventType eventTypeScheduled;

	// capacity and state
	private int capacity;
	private final PriorityQueue<InService> running = new PriorityQueue<>();
	private final Deque<Integer> freeServers = new ArrayDeque<>();

	// 'round-robin' hint for the next server to use
	private int rrHint = 0;

	// stats
	private int served = 0;
	private double busyTime = 0.0;
	private double totalWaitTime = 0.0;

	// per-server stats
	private double[] perServerBusyTime;
	private int[] perServerServed;


	// ---------- Constructors ----------

	public ServicePoint(ContinuousGenerator gen, EventList eventList, EventType type) {
		this(gen, eventList, type, 1);
	}

	public ServicePoint(ContinuousGenerator generator, EventList eventList, EventType type, int capacity) {
		this(makeUniformGenerators(generator, Math.max(1, capacity)), eventList, type, Math.max(1, capacity));
	}

	public ServicePoint(ContinuousGenerator[] perServerGenerators, EventList eventList, EventType type) {
		this(perServerGenerators, eventList, type, perServerGenerators != null ? perServerGenerators.length : 1);
	}

	/**
	 * Create the service point with a waiting queue.
	 * @param perServerGenerators Random number generators for customer service time simulation, one per server
	 * @param eventList Simulator event list, needed for the insertion of service ready event
	 * @param type Event type for the service end event
	 * @param capacity Number of parallel servers (1for single server)
	 */
	private ServicePoint(ContinuousGenerator[] perServerGenerators, EventList eventList, EventType type, int capacity) {
		this.eventList = eventList;
		this.eventTypeScheduled = type;
		this.capacity = Math.max(1, capacity);

		this.generators = (perServerGenerators != null && perServerGenerators.length == this.capacity)
				? perServerGenerators.clone()
				: makeUniformGenerators(perServerGenerators != null && perServerGenerators.length > 0
				? perServerGenerators[0] : null, this.capacity);

		// queues per server
		this.queues = new LinkedList[this.capacity];
		for (int i = 0; i < this.capacity; i++) {
			this.queues[i] = new LinkedList<>();
		}

		this.perServerBusyTime = new double[this.capacity];
		this.perServerServed = new int[this.capacity];

		for (int i = 0; i < this.capacity; i++) {
			this.freeServers.addLast(i);
			this.perServerBusyTime[i] = 0.0;
			this.perServerServed[i] = 0;
		}
	}


	// ---------- Static helpers ----------

	private static ContinuousGenerator[] makeUniformGenerators(ContinuousGenerator gen, int capacity) {
		ContinuousGenerator[] arr = new ContinuousGenerator[capacity];
		for (int i = 0; i < capacity; i++) arr[i] = gen;
		return arr;
	}

	private static double bump(double now) {
		double next = Math.nextUp(now);
		return Double.isFinite(next) ? next : now + 1e-9;
	}

	private static double future(double candidate, double now) {
		return (!Double.isFinite(candidate) || candidate <= now) ? bump(now) : candidate;
	}


	// ---------- Internal helpers ----------

	/**
	 * Select server for enqueue:
	 * - Prefer idle servers (initial placement only).
	 * - Otherwise choose the shortest queue with round-robin tie-break.
	 */
	private int selectServerForEnqueue() {
		if (!this.freeServers.isEmpty()) {
			for (int k = 0; k < this.capacity; k++) {
				int i = (this.rrHint + k) % this.capacity;
				if (this.freeServers.contains(i)) {
					this.rrHint = (i + 1) % this.capacity;
					return i;
				}
			}
		}

		// no idle servers, choose the one with the shortest queue
		int best = -1;
		int bestSize = Integer.MAX_VALUE;
		for (int k = 0; k < this.capacity; k++) {
			int i = (this.rrHint + k) % this.capacity;
			int size = this.queues[i].size();
			if (size < bestSize) {
				best = i;
				bestSize = size;
			}
		}
		if (best < 0) best = 0;
		this.rrHint = (best + 1) % this.capacity;
		return best;
	}


	// ---------- Queue operations ----------

	/**
	 * Add a customer to the service point queue.
	 *
	 * @param a Customer to be queued
	 */
	public synchronized void addQueue(Customer a) {	// The first customer of the queue is always in service
		int sid = selectServerForEnqueue();
		this.queues[sid].addLast(new QItem(a, Clock.getInstance().getClock()));
	}


	// ---------- Service lifecycle ----------

	/**
	 * Try to start a new service, if the service point is not busy and there is customers on the queue
	 * @param now Current time
	 * @return StartInfo if a new service was started, null otherwise
	 */
	public synchronized StartInfo tryStart(double now) {
		if (this.freeServers.isEmpty()) return null;

		int trials = availableSlots();
		int chosenSid = -1;

		// find a free server with a waiting queue
		for (int i = 0; i < trials; i++) {
			int sid = this.freeServers.removeFirst();
			if (!this.queues[sid].isEmpty()) {
				chosenSid = sid;
				break;
			} else {
				this.freeServers.addLast(sid);
			}
		}

		if (chosenSid < 0) return null;

		QItem qi = this.queues[chosenSid].removeFirst();
		Customer c = qi.customer;

		double wait = Math.max(0.0, now - qi.enqueuedAt);
		this.totalWaitTime += wait;

		double st = (this.generators[chosenSid] != null) ? this.generators[chosenSid].sample() : 0.0;
		double end = future(now + st, now);
		this.running.add(new InService(c, now, end, chosenSid));

		this.eventList.add(new Event(this.eventTypeScheduled, end));

		return new StartInfo(c, st, end, chosenSid);
	}

	/**
	 * Finish the current service, customer is removed from the service point
	 * @param now Current time
	 * @return Customer who has been serviced, null if no customer was in service
	 */
	public synchronized EndInfo finishService(double now) {
		InService top = this.running.peek();

		if (top == null || (top.endTime - now) > 1e-9) return null;

		this.running.poll();
		this.served++;

		double sv = Math.max(0.0, top.endTime - top.startTime);
		this.busyTime += sv;
		this.perServerBusyTime[top.serverId] += sv;
		this.perServerServed[top.serverId]++;

		this.freeServers.addLast(top.serverId);
		return new EndInfo(top.customer, top.serverId, top.startTime, top.endTime);
	}


	// ---------- Getters and analytics ----------

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
		int sum = 0;
		for (LinkedList<QItem> q : this.queues) sum += q.size();
		return sum;
	}

	/** Get the number of currently busy servers
	 * @return number of currently busy servers
	 */
	public synchronized int availableSlots() {
		return this.freeServers.size();
	}

	/** Get the time when the current service will end
	 * @return time when the current service will end, or Double.POSITIVE_INFINITY if no service is ongoing
	 */
	public synchronized double nextEndTime() {
		InService top = this.running.peek();
		return top != null ? top.endTime : Double.POSITIVE_INFINITY;
	}


	// analytics
	public double getAverageServiceTime() {
		return this.served > 0 ? this.busyTime / this.served : 0.0;
	}

	public double getTotalWaitTime() {
		return this.totalWaitTime;
	}

	public double getAverageWaitTime() {
		return this.served > 0 ? this.totalWaitTime / this.served : 0.0;
	}

	public synchronized double[] getPerServerBusyTimeSnapshot() {
		double[] copy = new double[this.perServerBusyTime.length];
		System.arraycopy(this.perServerBusyTime, 0, copy, 0, copy.length);
		return copy;
	}

	public synchronized int[] getPerServerServedSnapshot() {
		int[] copy = new int[this.perServerServed.length];
		System.arraycopy(this.perServerServed, 0, copy, 0, copy.length);
		return copy;
	}
}
