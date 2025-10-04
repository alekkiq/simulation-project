package simu.controller;

import javafx.application.Platform;
import simu.framework.IEngine;
import simu.model.EngineMod;
import simu.model.ISnapshotListener;
import simu.model.SimParameters;
import simu.view.ISimulatorUI;
import simu.view.IVisualisation;
import simu.framework.Clock;

public class Controller implements IControllerVtoM, IControllerMtoV {   // NEW
	private IEngine engine;
	private ISimulatorUI ui;
    private SimParameters params;
	
	public Controller(ISimulatorUI ui, SimParameters params) {
		this.ui = ui;
        this.params = params;
	}

	/* Engine control: */
	@Override
	public void startSimulation() {
		// reset clock so that multiple runs work correctly
		Clock.getInstance().setClock(0.0);

		var options = params.toConfig();
		engine = new EngineMod(options, this);
		if (engine instanceof EngineMod em) {
			var vis = ui.getVisualisation();
			if (vis instanceof ISnapshotListener listener) {
				em.setSnapshotListener(listener);
			}
		}
		engine.setSimulationTime(ui.getTime());
		engine.setDelay(ui.getDelay());
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


	/* Simulation results passing to the UI
	 * Because FX-UI updates come from engine thread, they need to be directed to the JavaFX thread
	 */
	@Override
	public void showEndTime(double time) {
		Platform.runLater(()->ui.setEndingTime(time));
	}

	@Override
	public void visualiseCustomer() {
		IVisualisation vis = ui.getVisualisation();
		if (vis == null) return;
		Platform.runLater(vis::newCustomer);
	}
}
