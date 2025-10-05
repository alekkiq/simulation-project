package simu.view;

public interface IVisualisation {
    void clearDisplay();
    void newCustomer(int id);
    void updateServicePoints(int numMechanics, int numWashers);
    void updateQueueLengths(int receptionQueue, int[] mechanicQueues, int[] washerQueues);
    void moveCustomerToMechanic(int id, int mechanicId);
    void moveCustomerToWasher(int id, int washerId);
    void customerExit(int id);
}
