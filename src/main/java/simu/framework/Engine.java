package simu.framework;

/**
 * Engine implement three-phase simulator. See <a href="https://www.jstor.org/stable/2584330">Three-Phase Simulator</a>
 *
 * This is a skeleton of a three-phase simulator. You need to implement abstract methods for your
 * purpose.
 */
public abstract class Engine extends Thread implements IEngine {
	private double simulationTime = 0;	// time when the simulation will be stopped
	private long delay = 0;
	private Clock clock;				// to simplify the code (clock.getClock() instead Clock.getInstance().getClock())
	protected EventList eventList;		// events to be processed are stored here

	/**
	 * Service Points are created in simu.model-package's class inheriting the Engine class
	 */
	public Engine() {
		clock = Clock.getInstance();	// to improve the speed of the simulation
		eventList = new EventList();
	}

	/**
	 * Define how long we will run the simulation
	 * @param time Ending time of the simulation
	 */
	public void setSimulationTime(double time) {	// define how long we will run the simulation
		simulationTime = time;
	}

	@Override // NEW
	public void setDelay(long time) {
		this.delay = time;
	}

	@Override // NEW
	public long getDelay() {
		return this.delay;
	}

	/**
	 * The starting point of the simulator. We will return when the simulation ends.
	 */
	@Override
	public void run() {
		initialize(); // creating, e.g., the first event

		while (simulate()){
			beforeCycle();
			delay(); // NEW
			clock.setClock(currentTime());
			runBEvents();
			tryCEvents();
		}

		results();
	}

	private void delay() { // NEW
		Trace.out(Trace.Level.INFO, "Delay " + delay);
		try {
			sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Execute all B-events (bounded to time) at the current time removing them from the event list.
	 */
	private void runBEvents() {
		while (eventList.getNextEventTime() == clock.getClock()){
			runEvent(eventList.remove());
		}
	}

	/**
	 * @return Earliest event time at the event list
	 */
	private double currentTime(){
		return eventList.getNextEventTime();
	}

	/**
	 * @return logical value whether we should continue simulation
	 */
	private boolean simulate(){
		return clock.getClock() < simulationTime;
	}

	/**
	 * Execute event actions (e.g., removing customer from the queue)
	 * Defined in simu.model-package's class who is inheriting the Engine class
	 *
	 * @param t The event to be executed
	 */
	protected abstract void runEvent(Event t);

	/**
	 * Execute all possible C-events (conditional events)
	 * Defined in simu.model-package's class who is inheriting the Engine class
	 */
	protected abstract void tryCEvents();

	/**
	 * Set all data structures to initial values
	 * Defined in simu.model-package's class who is inheriting the Engine class
	 */
	protected abstract void initialize();

	/**
	 * Show/analyze measurement parameters collected during the simulation.
	 * This method is called at the end of the simulation.
	 * Defined in simu.model-package's class who is inheriting the Engine class
	 */
	protected abstract void results();

	/**
	 * This method is called at the beginning of each simulation cycle
	 * Can be used e.g., for updating a visualisation
	 * Defined in simu.model-package's class who is inheriting the Engine class
	 */
	protected abstract void beforeCycle();
}