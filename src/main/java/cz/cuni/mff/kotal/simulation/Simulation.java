package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
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
	private final Algorithm algorithm;

	private final long newAgentsMinimum;
	private final long newAgentsMaximum;
	private final List<Long> distribution;

	private long time;

	private Consumer<Map<Long, Agent>> guiCallback;


	public Simulation(Graph intersectionGraph, Algorithm algorithm) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		time = 0;
		newAgentsMinimum = AgentsMenuTab1.getNewAgentsMinimum().getValue();
		newAgentsMaximum = AgentsMenuTab1.getNewAgentsMaximum().getValue();
		distribution = AgentsMenuTab1.getDirectionDistribution().getChildren().stream().map(node -> ((AgentsMenuTab1.DirectionSlider) node).getValue()).collect(Collectors.toList());
	}

	public Simulation(Graph intersectionGraph, Algorithm algorithm, long newAgentsMinimum, long newAgentsMaximum, List<Long> distribution) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		time = 0;
		this.newAgentsMinimum = newAgentsMinimum;
		this.newAgentsMaximum = newAgentsMaximum;
		this.distribution = distribution;
	}

	public void tick(long delay) {
		try {
			// TODO add pause support
			while (true) {
				Set<Agent> newAgents = generateAgents(allAgents.size());
				currentAgents.addAll(newAgents);
				allAgents.putAll(newAgents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
				algorithm.planAgents(newAgents);

				updateAgents();
				Thread.sleep(delay);
				time++;
				IntersectionMenu.getStepsLabel().setText(String.valueOf(time));
				System.out.println(time);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected Set<Agent> generateAgents(int id) {
		assert (distribution.stream().reduce(0L, Long::sum) == 100);
		long newAgentsCount = generateRandomLong(newAgentsMinimum, newAgentsMaximum);

		Set<Agent> newAgents = new HashSet<>();

		for (long i = 0; i < newAgentsCount; i++) {
			long entryValue = generateRandomLong(100),
				exitValue = generateRandomLong(100);

			Agent newAgent = generateAgent(id++, entryValue, exitValue);
			newAgents.add(newAgent);
		}

		return newAgents;
	}

	protected Agent generateAgent(int id, long entryValue, long exitValue) {
		int entryDirection, exitDirection;
		for (entryDirection = 0; entryDirection < distribution.size() - 1 && entryValue >= distribution.get(entryDirection); entryValue -= distribution.get(entryDirection++)) ;
		List<Vertex> entries = intersectionGraph.getEntryExitVertices().get(entryDirection).stream().filter(e -> e.getType().isEntry()).collect(Collectors.toList());
		GraphicalVertex entry = (GraphicalVertex) entries.get(generateRandomInt(entries.size() - 1));

		for (exitDirection = 0; exitDirection < distribution.size() && (exitValue >= distribution.get(exitDirection) || exitDirection == entryDirection); exitValue -= distribution.get(exitDirection++)) {
			if (entryDirection == exitDirection) {
				exitValue -= distribution.get(exitDirection);
				if (exitValue <= 0) {
					if (exitDirection > 0) {
						exitDirection--;
					} else {
						exitDirection++;
					}
					break;
				} else {
					exitDirection++;
				}
			}
		}
		if (exitDirection >= distribution.size()) {
			exitDirection = distribution.size() - 1;
			while (distribution.get(--exitDirection) == 0) ;
		}
		List<Vertex> exits = intersectionGraph.getEntryExitVertices().get(exitDirection).stream().filter(e -> !e.getType().isEntry()).collect(Collectors.toList());
		Vertex exit = exits.get(generateRandomInt(exits.size() - 1));

		// TODO add speed, length and width
		return new Agent(id, entry.getID(), exit.getID(), 1, time, 1, 1, entry.getX(), entry.getY());
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
		guiCallback.accept(currentAgents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
	}

	private long generateRandomLong(long maximum) {
		return generateRandomLong(0, maximum);
	}

	private int generateRandomInt(int maximum) {
		return (int) generateRandomLong(maximum);
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

	public void setGuiCallback(Consumer<Map<Long, Agent>> guiCallback) {
		this.guiCallback = guiCallback;
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
