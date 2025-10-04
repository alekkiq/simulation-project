package simu.model;

import simu.framework.*;

/**
 * Customer in a simulator
 *
 * TODO: This is to be implemented according to the requirements of the simulation model (data!)
 */
public class Customer {
	private double arrivalTime;
	private double removalTime;
	private int id;
	private static int i = 1;
	private static long sum = 0;

	public enum WashProgram {
		NONE, INTERIOR, EXTERIOR, BOTH
	}

	// routing decisions
	private boolean needsMechanic;
	private boolean needsWash;
	private WashProgram washProgram = WashProgram.NONE;

	// timestamps
	// naming explanation: (t -> time, QIn -> Queue In)
	public double tReceptionQIn, tReceptionStart, tReceptionEnd;
	public double tMechanicQIn, tMechanicStart, tMechanicEnd;
	public double tWashQIn, tWashStart, tWashEnd;
	public double tDeparture;

	/**
	 * Create a unique customer
	 */
	public Customer() {
	    this.id = i++;
	    
		this.arrivalTime = Clock.getInstance().getClock();
		Trace.out(Trace.Level.INFO, "New customer #" + this.id + " arrived at  " + this.arrivalTime);
	}

	/**
	 * Give the time when customer has been removed (from the system to be simulated)
	 * @return Customer removal time
	 */
	public double getRemovalTime() {
		return this.removalTime;
	}

	/**
	 * Mark the time when the customer has been removed (from the system to be simulated)
	 * @param removalTime Customer removal time
	 */
	public void setRemovalTime(double removalTime) {
		this.removalTime = removalTime;
	}

	/**
	 * Give the time when the customer arrived to the system to be simulated
	 * @return Customer arrival time
	 */
	public double getArrivalTime() {
		return this.arrivalTime;
	}

	/**
	 * Mark the time when the customer arrived to the system to be simulated
	 * @param arrivalTime Customer arrival time
	 */
	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	/**
	 * Get the (unique) customer id
	 * @return Customer id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Check whether the customer needs mechanic service
	 * @return logical value indicating mechanic service need
	 */
	public boolean needsMechanic() {
		return this.needsMechanic;
	}

	/**
	 * Set whether the customer needs mechanic service
	 * @param needsMechanic logical value indicating mechanic service need
	 */
	public void setNeedsMechanic(boolean needsMechanic) {
		this.needsMechanic = needsMechanic;
	}

	/**
	 * Check whether the customer needs wash service
	 * @return logical value indicating wash service need
	 */
	public boolean needsWash() {
		return this.needsWash;
	}

	/**
	 * Set whether the customer needs wash service
	 * @param needsWash logical value indicating wash service need
	 */
	public void setNeedsWash(boolean needsWash) {
		this.needsWash = needsWash;
	}

	/**
	 * Get the wash program of the customer
	 * @return Wash program
	 */
	public WashProgram getWashProgram() {
		return this.washProgram;
	}

	/**
	 * Set the wash program of the customer
	 * @param washProgram Wash program
	 */
	public void setWashProgram(WashProgram washProgram) {
		this.washProgram = washProgram;
	}

	/**
	 * Indicate whether all needs of the customer have been served
	 * @return logical value indicating whether all needs of the customer have been served
	 */
	public boolean allDone() {
		boolean mechDone = !this.needsMechanic || (this.tMechanicEnd > 0);
		boolean washDone = !this.needsWash || (this.tWashEnd > 0);
		boolean receptionDone = (this.tReceptionEnd > 0);
		return mechDone && washDone && receptionDone;
	}

	/**
	 * Report the measured variables of the customer. In this case to the diagnostic output.
	 */
	public void reportResults() {
		Trace.out(Trace.Level.INFO, "\nCustomer " + this.id + " ready! ");
		Trace.out(Trace.Level.INFO, "Customer "   + this.id + " arrived: " + this.arrivalTime);
		Trace.out(Trace.Level.INFO,"Customer "    + this.id + " removed: " + this.removalTime);
		Trace.out(Trace.Level.INFO,"Customer "    + this.id + " stayed: "  + (this.removalTime - this.arrivalTime));

		sum += (this.removalTime - this.arrivalTime);
		double mean = sum/id;
		System.out.println("Current mean of the customer service times " + mean);
	}
}
