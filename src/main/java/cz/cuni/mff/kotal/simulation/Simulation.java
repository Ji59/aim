package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentsMenuTab1;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class Simulation {
	private final Graph intersectionGraph;
	private final Map<Long, Agent> allAgents = new HashMap<>();
	private final Set<Agent> currentAgents = new HashSet<>();
	private final long newAgentsMinimum;
	private final long newAgentsMaximum;
	private final List<Long> distribution;
	private long time;
	private Consumer<Map<Long, Agent>> callback;


	public Simulation(Graph intersectionGraph) {
		this.intersectionGraph = intersectionGraph;
		time = 0;
		newAgentsMinimum = AgentsMenuTab1.getNewAgentsMinimum().getValue();
		newAgentsMaximum = AgentsMenuTab1.getNewAgentsMaximum().getValue();
		distribution = AgentsMenuTab1.getDirectionDistribution().getChildren().stream().map(node -> ((AgentsMenuTab1.DirectionSlider) node).getValue()).collect(Collectors.toList());
	}

	public Simulation(Graph intersectionGraph, long newAgentsMinimum, long newAgentsMaximum, List<Long> distribution) {
		this.intersectionGraph = intersectionGraph;
		time = 0;
		this.newAgentsMinimum = newAgentsMinimum;
		this.newAgentsMaximum = newAgentsMaximum;
		this.distribution = distribution;
	}

	public void tick(long delay) {
		try {
			while (true) {

				Thread.sleep(delay);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void generateAgents() {
		long newAgentsCount = generateRandomLong(newAgentsMinimum, newAgentsMaximum);

		for (long i = 0; i < newAgentsCount; i++) {
			long entryValue = generateRandomLong(100);
			int entryDirection, exitDirection;
			for (entryDirection = 0; entryValue > 0; entryValue -= distribution.get(entryDirection++)) ;
			List<Vertex> entries = intersectionGraph.getEntryExitVertices().get(entryDirection).stream().filter(e -> e.getType().isEntry()).collect(Collectors.toList());
			GraphicalVertex entry = (GraphicalVertex) entries.get((int) generateRandomLong(entries.size() - 1));

			long exitValue = generateRandomLong(distribution.get(entryDirection));
			for (exitDirection = 0; exitValue > 0; exitValue -= distribution.get(exitDirection++)) {
				if (entryDirection == exitDirection) {
					exitValue -= distribution.get(exitDirection);
					if (exitValue <= 0) {
						exitDirection--;
						break;
					} else {
						exitDirection++;
					}
				}
			}
			if (exitDirection >= distribution.size()) {
				exitDirection = distribution.size() - 2;
			}
			List<Vertex> exits = intersectionGraph.getEntryExitVertices().get(exitDirection).stream().filter(e -> !e.getType().isEntry()).collect(Collectors.toList());
			Vertex exit = exits.get((int) generateRandomLong(entries.size() - 1));


			Agent newAgent = new Agent(allAgents.size(), entry.getID(), exit.getID(), 1, time, 1, 1, entry.getX(), entry.getY());
			allAgents.put(newAgent.getId(), newAgent);
			currentAgents.add(newAgent);
		}
	}

	private void updateAgents() {
		for (Agent agent : currentAgents) {
			try {
				agent.computeNextXY(time);
			} catch (IndexOutOfBoundsException e) {
				// agent doesn't exist in this step
				currentAgents.remove(agent);
			}
		}
		callback.accept(currentAgents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
	}

	private long generateRandomLong(long maximum) {
		return generateRandomLong(0, maximum);
	}

	private long generateRandomLong(long minimum, long maximum) {
		double random = Math.random();
		if (random == 0) {
			return 0;
		} else return Math.round(random * (maximum - minimum + 1) + minimum - 0.5);
	}

	public Graph getIntersectionGraph() {
		return intersectionGraph;
	}

	public Collection<Agent> getAllAgents() {
		return allAgents.values();
	}

	public Collection<Agent> getCurrentAgents() {
		return currentAgents;
	}

	public Agent getAgent(long id) {
		return allAgents.get(id);
	}

	public void setCallback(Consumer<Map<Long, Agent>> callback) {
		this.callback = callback;
	}

	public long getNewAgentsMinimum() {
		return newAgentsMinimum;
	}

	public long getNewAgentsMaximum() {
		return newAgentsMaximum;
	}

	public List<Long> getDistribution() {
		return distribution;
	}

	public long getTime() {
		return time;
	}
}
