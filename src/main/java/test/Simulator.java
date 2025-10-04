package test;

// import simu.model.MyEngine;
import simu.view.SimulatorGUI;

/**
 * Command-line type User Interface
 * With setTraceLevel() you can control the number of diagnostic messages printed to the console.
 */
public class Simulator {
	public static void main(String[] args) {
		//Trace.setTraceLevel(Level.INFO);
//
		//Engine m = new EngineMod(o);
		//m.setSimulationTime(100000.0);
		//m.run();
		SimulatorGUI.main(args);
	}
}
