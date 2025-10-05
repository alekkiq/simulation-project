package simu.controller;

import simu.model.SimulationData;

/* interface for the engine */
public interface IControllerMtoV {
    public void visualiseCustomer(int id);
    public void visualiseCustomerToMechanic(int id, int mechanicId);
    public void visualiseCustomerToWasher(int id, int washerId);
    public void visualiseCustomerExit(int id);
    public void updateServicePoints(int numMechanics, int numWashers);
    public void updateQueueLengths(int receptionQueue, int[] mechanicQueues, int[] washerQueues);
    public void simulationFinished(double endTime, SimulationData data);
}
