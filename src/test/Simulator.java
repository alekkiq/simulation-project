package test;

import eduni.distributions.ContinuousGenerator;
import eduni.distributions.Negexp;
import eduni.distributions.Normal;
import simu.config.SimulationOptions;
import simu.framework.Engine;
import simu.framework.Trace;
import simu.framework.Trace.Level;
// import simu.model.MyEngine;
import simu.model.EngineMod;
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
