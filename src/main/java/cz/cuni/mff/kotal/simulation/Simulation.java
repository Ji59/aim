package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentParametersMenuTab4;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentsMenuTab1;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.frontend.simulation.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.MyGenerator.*;


public class Simulation {
	private final SimulationGraph intersectionGraph;

	private final Map<Long, Agent> allAgents = new HashMap<>();
	private final Set<Agent> currentAgents = Collections.synchronizedSet(new HashSet<>());
	private final Algorithm algorithm;

	private final long newAgentsMinimum;
	private final long newAgentsMaximum;
	private final List<Long> distribution;

	private long time;
	private Timer timer;

	private Consumer<Pair<Long, Map<Long, Agent>>> guiCallback;


	public Simulation(SimulationGraph intersectionGraph, Algorithm algorithm) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		time = 0;
		newAgentsMinimum = AgentsMenuTab1.getNewAgentsMinimum().getValue();
		newAgentsMaximum = AgentsMenuTab1.getNewAgentsMaximum().getValue();
		distribution = AgentsMenuTab1.getDirectionDistribution().getChildren().stream().map(node -> ((AgentsMenuTab1.DirectionSlider) node).getValue()).collect(Collectors.toList());
	}

	public Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, long newAgentsMinimum, long newAgentsMaximum, List<Long> distribution) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		time = 0;
		this.newAgentsMinimum = newAgentsMinimum;
		this.newAgentsMaximum = newAgentsMaximum;
		this.distribution = distribution;
	}


	public void start(long period) {
		startAfter(0, period);
	}

	public void startAfter(long delay, long period) {
		timer = new Timer();
		timer.scheduleAtFixedRate(getTimerTask(), delay, period);
	}

	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@NotNull
	private TimerTask getTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
				Set<Agent> newAgents = generateAgents(allAgents.size());
				algorithm.planAgents(newAgents);
				allAgents.putAll(newAgents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
				currentAgents.addAll(newAgents);
				updateAgents();
				time++;
				IntersectionMenu.setStep(time);
				System.out.println(time);
			}
		};
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
		// TODO solve problem with multiple agents arriving at same time
		for (entryDirection = 0; entryDirection < distribution.size() - 1 && entryValue >= distribution.get(entryDirection); entryDirection++) {
			entryValue -= distribution.get(entryDirection);
		}
		while (distribution.get(entryDirection) == 0) {
			entryDirection--;
		}

		List<Vertex> entries = intersectionGraph.getEntryExitVertices().get(entryDirection).stream().filter(e -> e.getType().isEntry()).collect(Collectors.toList());
		GraphicalVertex entry = (GraphicalVertex) entries.get(generateRandomInt(entries.size() - 1));

		for (exitDirection = 0; exitDirection < distribution.size() - 1 && (exitValue >= distribution.get(exitDirection) || exitDirection == entryDirection); exitValue -= distribution.get(exitDirection++)) {
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
		}
		while (exitDirection >= 0 && exitDirection != entryDirection && distribution.get(exitDirection) == 0) {
			exitDirection--;
		}
		if (exitDirection < 0) {
			exitDirection = 0;
			while (exitDirection < distribution.size() && (exitDirection == entryDirection || distribution.get(exitDirection) == 0)) {
				exitDirection++;
			}
			if (exitDirection >= distribution.size()) {
				exitDirection = entryDirection == 0 ? 1 : 0;
			}
		}
		List<Vertex> exits = intersectionGraph.getEntryExitVertices().get(exitDirection).stream().filter(e -> !e.getType().isEntry()).collect(Collectors.toList());
		Vertex exit = exits.get(generateRandomInt(exits.size() - 1));

		double maxDeviation = AgentParametersMenuTab4.getSpeedDeviation().getValue();

		long minimalSpeed = AgentParametersMenuTab4.getMinimalSpeed().getValue(),
			maximalSpeed = AgentParametersMenuTab4.getMaximalSpeed().getValue();
		double speed = generateWithDeviation(minimalSpeed, maximalSpeed, maxDeviation);

		// TODO remake length and width
		long minimalLength = AgentParametersMenuTab4.getMinimalSizeLength().getValue(),
			maximalLength = AgentParametersMenuTab4.getMaximalSizeLength().getValue(),
			minimalWidth = AgentParametersMenuTab4.getMinimalSizeWidth().getValue(),
			maximalWidth = AgentParametersMenuTab4.getMaximalSizeWidth().getValue();

		double cellSize = intersectionGraph.getCellSize(),
			width = Math.max(generateWithDeviation(minimalWidth, maximalWidth, maxDeviation) - 0.5, 0.5) * cellSize,
			length = Math.max(generateWithDeviation(minimalLength, maximalLength, maxDeviation) - 0.5, 0.5) * cellSize;

		return new Agent(id, entry.getID(), exit.getID(), speed, time, length, width, entry.getX(), entry.getY());
	}

	private void updateAgents() {
		Iterator<Agent> currentAgentsIterator = currentAgents.iterator();
		while (currentAgentsIterator.hasNext()) {
			Agent agent = currentAgentsIterator.next();
			try {
				agent.computeNextXY(time, intersectionGraph.getVerticesWithIDs());
			} catch (IndexOutOfBoundsException e) {
				// agent doesn't exist in this step
				// TODO remove log
				System.out.println("Removing: " + agent.getId());
				currentAgentsIterator.remove();
			}
		}
		guiCallback.accept(new Pair<>(time, currentAgents.stream().collect(Collectors.toMap(Agent::getId, Function.identity()))));
	}

	public boolean isRunning() {
		return timer != null;
	}

	public SimulationGraph getIntersectionGraph() {
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

	public void setGuiCallback(Consumer<Pair<Long, Map<Long, Agent>>> guiCallback) {
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
