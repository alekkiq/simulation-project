package simu.controller;

import javafx.application.Platform;
import simu.config.SimulationOptions;
import simu.database.SimulationDataDAO;
import simu.framework.Clock;
import simu.framework.IEngine;
import simu.model.EngineMod;
import simu.model.SimParameters;
import simu.model.SimulationData;
import simu.view.ISimulatorUI;
import simu.view.Visualisation;

public class Controller implements IControllerVtoM, IControllerMtoV {   // NEW
	private EngineMod engine;
	private ISimulatorUI ui;
    private SimParameters params;
    private SimulationDataDAO dao = new SimulationDataDAO();

    public Controller(ISimulatorUI ui, SimParameters params) {
        this.ui = ui;
        this.params = params;
    }

    /* Engine control: */
    @Override
    public void startSimulation() {
        Clock.getInstance().setClock(0.0); // reset clock so that multiple simulations work correctly

        SimulationOptions options = params.toConfig();

        engine = new EngineMod(options,this); // Pass SimParameters to EngineMod
        engine.setSimulationTime(ui.getTime());
        engine.setDelay(ui.getDelay());
        // Update visualization with initial service point configuration
        updateServicePoints(options.getMechanicServers(), options.getWashServers());
        ui.getVisualisation().clearDisplay();
        ((Thread) engine).start();
    }

    @Override
    public void decreaseSpeed() { // hidastetaan moottorisäiettä
        engine.setDelay((long)(engine.getDelay()*1.10));
    }

    @Override
    public void increaseSpeed() { // nopeutetaan moottorisäiettä
        engine.setDelay((long)(engine.getDelay()*0.9));
    }

    @Override
    public void simulationFinished(double endTime, SimulationData data) {
        Platform.runLater(() -> ui.onSimulationFinished(endTime, data));

        Thread t = new Thread(() -> {
           try {
               dao.persist(data);
           } catch (Exception e) {
               // TODO: more graceful error handling (e.g. alert in UI)
               e.printStackTrace();
           }
        }, "db-persist-thread");
        t.setDaemon(true);
        t.start();
    }


    /* Simulation results passing to the UI
     * Because FX-UI updates come from engine thread, they need to be directed to the JavaFX thread
     */
    @Override
    public void showEndTime(double time) {
        Platform.runLater(()->ui.setEndingTime(time));
    }

    @Override
    public void visualiseCustomer() {
        Platform.runLater(() -> ui.getVisualisation().newCustomer());
    }

    @Override
    public void visualiseCustomerToMechanic(int id, int mechanicId) {
        Platform.runLater(() -> ((Visualisation)ui.getVisualisation()).moveCustomerToMechanic(id, mechanicId));
    }

    @Override
    public void visualiseCustomerToWasher(int id, int washerId) {
        Platform.runLater(() -> ((Visualisation)ui.getVisualisation()).moveCustomerToWasher(id, washerId));
    }

    @Override
    public void visualiseCustomerExit(int id) {
        Platform.runLater(() -> ((Visualisation)ui.getVisualisation()).customerExit(id));
    }

    @Override
    public void updateServicePoints(int numMechanics, int numWashers) {
        Platform.runLater(() -> ui.getVisualisation().updateServicePoints(numMechanics, numWashers));
    }

    @Override
    public void updateQueueLengths(int receptionQueue, int[] mechanicQueues, int[] washerQueues) {
        Platform.runLater(() -> ((Visualisation)ui.getVisualisation()).updateQueueLengths(receptionQueue, mechanicQueues, washerQueues));
    }
}
