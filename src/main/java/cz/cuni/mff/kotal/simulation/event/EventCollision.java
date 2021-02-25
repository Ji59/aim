package cz.cuni.mff.kotal.simulation.event;


import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.Edge;

import java.math.BigInteger;


public class EventCollision extends Event {

	private final Edge location;
	private final Agent collisionAgent;

	public EventCollision(Action action, double time, Agent agent0, Agent agent1, Edge e) {
		super(action, time, agent0);
		assert action == Action.COLLISION;
		collisionAgent = agent1;
		location = e;
	}

	public EventCollision(double time, Agent agent0, Agent agent1, Edge e) {
		super(Action.COLLISION, time, agent0);
		collisionAgent = agent1;
		location = e;
	}

	public Edge getLocation() {
		return location;
	}
}