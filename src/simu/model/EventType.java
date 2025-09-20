package simu.model;

import simu.framework.IEventType;

/**
 * Event types are defined by the requirements of the simulation model
 *
 * TODO: This must be adapted to the actual simulator
 */
public enum EventType implements IEventType {
	ARRIVAL,
	RECEPTION_START,
	RECEPTION_END,
	MECHANIC_START,
	MECHANIC_END,
	WASH_START,
	WASH_END,
	DEPARTURE
}
