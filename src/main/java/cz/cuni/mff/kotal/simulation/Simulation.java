package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.frontend.simulation.Agent;
import cz.cuni.mff.kotal.simulation.event.Event;
import cz.cuni.mff.kotal.simulation.graph.Graph;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;


public class Simulation {
	protected final Graph intersectionGraph;
	protected final Queue<Event> heap = new PriorityQueue<>();
	protected final Set<Agent> agents = new HashSet<>();

	public Simulation(Graph intersectionGraph) {
		this.intersectionGraph = intersectionGraph;
	}

	public Graph getIntersectionGraph() {
		return intersectionGraph;
	}

	public Queue<Event> getHeap() {
		return heap;
	}

	public Set<Agent> getAgents() {
		return agents;
	}
}
