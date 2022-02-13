package cz.cuni.mff.kotal.simulation;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentParametersMenuTab4;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentsMenuTab1;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.helpers.MyGenerator.*;
import static cz.cuni.mff.kotal.helpers.MyNumberOperations.myModulo;


/**
 * Class representing simulation.
 */
public class Simulation {
	private final SimulationGraph intersectionGraph;

	private final Map<Long, Agent> allAgents = new HashMap<>();
	private final Map<Long, List<Agent>> delayedAgents = new HashMap<>();
	private final Algorithm algorithm;

	private final long maximumSteps;

	private final long newAgentsMinimum;
	private final long newAgentsMaximum;
	private final List<Long> distribution;

	private final boolean randomEntry = false;
	private final boolean generateEntry = true;
	private final boolean generateExit = false; // TODO

	private long step = 0;
	private long generatedStep = 0;
	private long agentsDelay;
	private long agentsRejected;
	private long collisions;

	private long startTime;
	private long period;
	private Timer timer;

	private final SimulationAgents simulationAgents;

	// TODO
	public static long maximumDelay = 16;


	/**
	 * Create new simulation.
	 *
	 * @param intersectionGraph Graph of the simulation
	 * @param algorithm         Algorithm used in computation
	 * @param simulationAgents  Agents simulation pane
	 */
	public Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		this.simulationAgents = simulationAgents;
		this.maximumSteps = AgentsMenuTab1.getSteps();
		newAgentsMinimum = AgentsMenuTab1.getNewAgentsMinimum().getValue();
		distribution = AgentsMenuTab1.getDirectionDistribution().getChildren().stream().map(node -> ((AgentsMenuTab1.DirectionSlider) node).getValue()).collect(Collectors.toList());

//		newAgentsMaximum = AgentsMenuTab1.getNewAgentsMaximum().getValue();
		newAgentsMaximum = Math.min(AgentsMenuTab1.getNewAgentsMaximum().getValue(), distribution.stream().filter(d -> d > 0).count());

		startTime = System.nanoTime();
		maximumDelay = intersectionGraph.getGranularity() * intersectionGraph.getEntryExitVertices().size();

		intersectionGraph.getEntryExitVertices().values().forEach(
			directionList -> directionList.stream()
				.filter(vertex -> vertex.getType().isEntry())
				.forEach(entry -> delayedAgents.put(entry.getID(), new ArrayList<>()))
		);
	}

	/**
	 * Create new simulation.
	 *
	 * @param intersectionGraph Graph of the simulation
	 * @param algorithm         Algorithm used in computation
	 * @param newAgentsMinimum  Minimal amount of new agents in each step
	 * @param newAgentsMaximum  Maximal amount of new agents in each step
	 * @param distribution      Entries usage distribution
	 * @param simulationAgents  Agents simulation pane
	 */
	public Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, long maximumSteps, long newAgentsMinimum, long newAgentsMaximum, List<Long> distribution, SimulationAgents simulationAgents) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		this.maximumSteps = maximumSteps;
		this.simulationAgents = simulationAgents;
		this.newAgentsMinimum = newAgentsMinimum;
		this.distribution = distribution;

		this.newAgentsMaximum = Math.min(newAgentsMaximum, distribution.stream().filter(d -> d > 0).count());
	}


	/**
	 * Start this simulation.
	 * Create timer for agents generation and planning.
	 *
	 * @param period Delay between steps
	 */
	public void start(long period) {
		this.period = period;
		simulationAgents.setSimulation(this);

		long delay = isRunning() ? period : 0L;

		if (!isRunning()) {
			IntersectionMenu.setAgents(0);
			IntersectionMenu.setDelay(0);
			IntersectionMenu.setCollisions(0);
		}

		timer = new Timer();
		timer.scheduleAtFixedRate(getTimerTask(), delay, period);
		new Thread(() -> {
			try {
				long now = System.nanoTime();
				Thread.sleep(delay);
				long noNow = System.nanoTime();
				System.out.println((noNow - now) / 1_000_000 + "; " + delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			simulationAgents.resumeSimulation(this);
		}).start();
	}

	/**
	 * Stop this simulation.
	 */
	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
			simulationAgents.pauseSimulation();
		}
	}

	/**
	 * Reset this simulation.
	 */
	public void reset() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		delayedAgents.values().forEach(Collection::clear);
	}

	/**
	 * Create new timer task for creating new agents.
	 *
	 * @return New timer task
	 */
	@NotNull
	private TimerTask getTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
				step++;
				long currentStep = step; // TODO

				long stepTime = System.nanoTime();
				if (maximumSteps > 0 && currentStep > maximumSteps) {
					if (delayedAgents.isEmpty()) {
						stop();
					}
				} else {
					List<Agent> newAgents = generateAgents(allAgents.size());
					allAgents.putAll(newAgents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
					newAgents.forEach(agent -> delayedAgents.get(agent.getEntry()).add(agent));
				}

				List<Agent> entriesAgents = delayedAgents.values().stream().map(entryList -> entryList.stream().findFirst().orElse(null)).filter(Objects::nonNull).toList();

				Collection<Agent> plannedAgents = algorithm.planAgents(entriesAgents, currentStep);
				plannedAgents.forEach(agent -> {
					simulationAgents.addAgent(stepTime, agent);
					agent.setPlannedTime(currentStep);
				});

				delayedAgents.values().parallelStream().forEach(entryList -> entryList.removeAll(plannedAgents));
				// TODO replace with iterator
				Set<Agent> rejectedAgents = delayedAgents.values().parallelStream().flatMap(Collection::parallelStream).filter(agent -> agent.getArrivalTime() < step - maximumDelay).collect(Collectors.toSet());
				delayedAgents.values().parallelStream().forEach(entryList -> entryList.removeAll(rejectedAgents));

				agentsRejected += rejectedAgents.size();
				agentsDelay += delayedAgents.values().parallelStream().mapToLong(Collection::size).sum();

				agentsDelay += plannedAgents.stream().mapToLong(agent -> agent.getPath().size() - intersectionGraph.getLines().get(agent.getEntry()).get(agent.getPath().get(agent.getPath().size() - 1)).size()).sum();

				IntersectionMenu.setAgents(allAgents.size());
				IntersectionMenu.setStep(currentStep);
				IntersectionMenu.setDelay(agentsDelay);
				IntersectionMenu.setRejections(agentsRejected);

//				System.out.println(step); // TODO
				assert step == currentStep;
			}
		};
	}

	/**
	 * Generate new agents with IDs starting from specified ID.
	 *
	 * @param id ID of first generated agent; increases with reach generated agent
	 * @return Set of newly generated agents
	 */
	protected List<Agent> generateAgents(int id) {
		assert (distribution.stream().reduce(0L, Long::sum) == 100);
		long newAgentsCount = generateRandomLong(newAgentsMinimum, newAgentsMaximum);

		List<Agent> newAgents = new ArrayList<>();
		Map<Integer, List<Vertex>> entries = getEntriesExitsMap(true);
		Map<Integer, List<Vertex>> exits = getEntriesExitsMap(false);

		for (long i = 0; i < newAgentsCount; i++) {
			long entryValue = generateRandomLong(100);
			long exitValue = generateRandomLong(100);

			Agent newAgent = generateAgent(id++, entryValue, exitValue, entries, exits);
			newAgents.add(newAgent);
		}

		if (!randomEntry && generateEntry) {
			Map<Long, List<Agent>> directionsAgents = new HashMap<>();
			int directions = intersectionGraph.getEntryExitVertices().size();
			for (long direction = 0; direction < directions; direction++) {
				directionsAgents.put(direction, new ArrayList<>());
			}
			for (Agent agent : newAgents) {
				directionsAgents.get(agent.getEntryDirection()).add(agent);
			}

			for (int entryDirection = 0; entryDirection < directions; entryDirection++) {
				List<Agent> directionAgents = directionsAgents.get((long) entryDirection);
//				directionAgents.sort((agent0, agent1) -> {
//					int directionDifference = Long.compare(myModulo(agent0.getEntryDirection(), directions), myModulo(agent1.getExitDirection(), directions));
//					if (directionDifference == 0) {
//						return Long.compare(agent0.getEntry(), agent1.getEntry());
//					}
//					return directionDifference;
//				});
				List<Vertex> directionEntries = intersectionGraph.getEntryExitVertices().get(entryDirection).parallelStream().filter(vertex -> vertex.getType().isEntry()).toList();

				for (Agent agent : directionAgents) {
					long relativeExitDirection = myModulo(agent.getExitDirection() - entryDirection, directions) - 1;
					int maxDirectionEntryIndex = directionEntries.size() - 1;
					long goldenEntryIndex = maxDirectionEntryIndex - maxDirectionEntryIndex * relativeExitDirection / (directions - 2);
					GraphicalVertex bestEntry = null;
					long bestPriority = Long.MAX_VALUE;
					for (int i = 0; i <= maxDirectionEntryIndex; i++) {
						GraphicalVertex entry = (GraphicalVertex) directionEntries.get(i);
						long queueSize = delayedAgents.get(entry.getID()).size();
						long priority = queueSize + Math.abs(goldenEntryIndex - i);
						if (priority < bestPriority || (priority == bestPriority && i <= goldenEntryIndex)) {
							bestEntry = entry;
							bestPriority = priority;
						}
					}
					assert bestEntry != null;
					agent.setEntry(bestEntry);
				}

//				Agent agent;
//				int highestEntryIndex = directionEntries.size() - 1;
//				int lowest;
//				for (lowest = 0; lowest < directionAgents.size() && myModulo((agent = directionAgents.get(lowest)).getExitDirection() - entryDirection, directions) < directions / 2; lowest++) {
//					agent.setEntry((GraphicalVertex) directionEntries.get(highestEntryIndex--));
//				}
//
//				int lowestEntryIndex = 0;
//				int highest;
//				for (highest = directionAgents.size() - 1; highest >= 0 && myModulo((agent = directionAgents.get(highest)).getExitDirection() + entryDirection, directions) > directions / 2; highest--) {
//					agent.setEntry((GraphicalVertex) directionEntries.get(lowestEntryIndex++));
//				}
//
//				for (; lowest <= highest; lowest++) {
//					agent = directionAgents.get(lowest);
//
//					int agentsRemaining = highest - lowest + 1;
//					int entriesRemaining = highestEntryIndex - lowestEntryIndex;
//					int entryIndexShift = entriesRemaining / agentsRemaining;
//					int entryIndex = entryIndexShift + lowestEntryIndex - 1;
//					lowestEntryIndex += entryIndexShift;
//					agent.setEntry((GraphicalVertex) directionEntries.get(entryIndex));
//				}
			}
		}

		return newAgents;
	}

	/**
	 * Generate new agent.
	 *
	 * @param id         ID of the new agent
	 * @param entryValue Value used for entry direction selection
	 * @param exitValue  Value used for exit direction selection
	 * @param entries    Map of entries from different directions
	 * @param exits      Map of exits from different directions
	 * @return Newly generated agent
	 */
	protected Agent generateAgent(int id, long entryValue, long exitValue, Map<Integer, List<Vertex>> entries, Map<Integer, List<Vertex>> exits) {
		// FIXME No agents from E to S in square
		// TODO generate only directions
		int entryDirection;
		int exitDirection;
		for (entryDirection = 0; entryDirection < distribution.size() - 1 && entryValue >= distribution.get(entryDirection); entryDirection++) {
			entryValue -= distribution.get(entryDirection);
		}

		int startingEntryDirection = entryDirection;
		while (entryDirection < entries.size() && (distribution.get(entryDirection) == 0 || entries.get(entryDirection).isEmpty())) {
			entryDirection++;
		}
		if (entryDirection >= entries.size()) {
			entryDirection = startingEntryDirection - 1;
			while (entryDirection >= 0 && distribution.get(entryDirection) == 0 || entries.get(entryDirection).isEmpty()) {
				entryDirection--;
			}
			if (entryDirection < 0) {
				entryDirection = startingEntryDirection;
			}
		}

		for (exitDirection = 0; exitDirection < distribution.size() && (exitValue >= distribution.get(exitDirection) || exitDirection == entryDirection); exitValue -= distribution.get(exitDirection++)) {
			if (entryDirection == exitDirection) {
				exitValue -= distribution.get(exitDirection);
				if (exitValue <= 0) {
					if (exitDirection < -distribution.get(exitDirection) / 2) {
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

		int startingExitDirection = exitDirection;
		if (exitDirection >= distribution.size()) {
			exitDirection = distribution.size() - 1;
		}
		while (exitDirection >= 0 && (exits.get(exitDirection).isEmpty() || exitDirection == entryDirection || distribution.get(exitDirection) == 0)) {
			exitDirection--;
		}
		if (exitDirection < 0) {
			exitDirection = startingExitDirection + 1;
			while (exitDirection < distribution.size() && (exits.get(exitDirection).isEmpty() || exitDirection == entryDirection || distribution.get(exitDirection) == 0)) {
				exitDirection++;
			}
			if (exitDirection >= distribution.size()) {
				if (entryDirection == startingExitDirection) {
					exitDirection = entryDirection == 0 ? 1 : 0;
				} else {
					exitDirection = startingExitDirection;
				}
			}
		}

		double maxDeviation = AgentParametersMenuTab4.getSpeedDeviation().getValue();

		long minimalSpeed = AgentParametersMenuTab4.getMinimalSpeed().getValue();
		long maximalSpeed = AgentParametersMenuTab4.getMaximalSpeed().getValue();
		double speed = generateWithDeviation(minimalSpeed, maximalSpeed, maxDeviation);

		// TODO remake length and width
		long minimalLength = AgentParametersMenuTab4.getMinimalSizeLength().getValue();
		long maximalLength = AgentParametersMenuTab4.getMaximalSizeLength().getValue();
		long minimalWidth = AgentParametersMenuTab4.getMinimalSizeWidth().getValue();
		long maximalWidth = AgentParametersMenuTab4.getMaximalSizeWidth().getValue();

		// TODO extract constants
		double width = Math.max(generateWithDeviation(minimalWidth, maximalWidth, maxDeviation) - 0.69, 0.1);
		double length = Math.max(generateWithDeviation(minimalLength, maximalLength, maxDeviation) - 0.42, 0.3);

		List<Vertex> directionEntries = entries.get(entryDirection);
		GraphicalVertex entry = null;
		double entryX = 0;
		double entryY = 0;
		if (randomEntry) {
			assert generateEntry;
			entry = (GraphicalVertex) directionEntries.get(generateRandomInt(directionEntries.size() - 1));
			directionEntries.remove(entry);
			entryX = entry.getX() * IntersectionModel.getPreferredHeight();
			entryY = entry.getY() * IntersectionModel.getPreferredHeight();
		} else {
			directionEntries.remove(0);
		}

		Vertex exit = null;
		if (generateExit) {
			List<Vertex> directionExits = exits.get(exitDirection);
			exit = directionExits.get(generateRandomInt(directionExits.size() - 1));
		}

		double arrivalTime = step + Math.exp(-Math.random()) * maxDeviation;

		return new Agent(id, randomEntry ? entry.getID() : -1, generateExit ? exit.getID() : -1, entryDirection, exitDirection, speed, arrivalTime, length, width, entryX, entryY);
	}

	/**
	 * Get map of entry / exit directions and vertices at the direction.
	 *
	 * @param isEntry If wanted entries or exits
	 * @return Map of entry direction numbers and vertices
	 */
	@NotNull
	Map<Integer, List<Vertex>> getEntriesExitsMap(boolean isEntry) {
		Map<Integer, List<Vertex>> entries = new HashMap<>();
		for (Map.Entry<Integer, List<Vertex>> entry : intersectionGraph.getEntryExitVertices().entrySet()) {
			entries.put(entry.getKey(), entry.getValue().stream().filter(v -> isEntry == v.getType().isEntry()).collect(Collectors.toCollection(LinkedList::new)));
		}
		return entries;
	}

	public void saveAgents(String path) throws IOException {
		FileWriter writer = new FileWriter(path, StandardCharsets.UTF_8, false);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		gson.toJson(allAgents.values(), writer);
		writer.close();
	}

	/**
	 * @return If this simulation is running
	 */
	public boolean isRunning() {
		return timer != null;
	}

	/**
	 * @return Graph of this simulation
	 */
	public SimulationGraph getIntersectionGraph() {
		return intersectionGraph;
	}

	/**
	 * @return All generated agents
	 */
	public Collection<Agent> getAllAgents() {
		return allAgents.values();
	}

	/**
	 * Get generated agent with specified ID.
	 *
	 * @param id ID of the agent
	 * @return Found agent
	 */
	public Agent getAgent(long id) {
		return allAgents.get(id);
	}

	/**
	 * @return Minimum agents to be generated
	 */
	public long getNewAgentsMinimum() {
		return newAgentsMinimum;
	}

	/**
	 * @return Maximum agents to be generated
	 */
	public long getNewAgentsMaximum() {
		return newAgentsMaximum;
	}

	/**
	 * @return Directions distribution
	 */
	public List<Long> getDistribution() {
		return distribution;
	}

	/**
	 * @return Actual simulation step
	 */
	public long getStep() {
		return step;
	}

	/**
	 * Increase number of collisions in this simulation.
	 * Write this value to GUI label.
	 */
	public void addCollision() {
		IntersectionMenu.setCollisions(++collisions);
	}

	/**
	 * @return System time of start of this simulation
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return Time Delay between steps in nanoseconds
	 */
	public long getPeriod() {
		return period;
	}
}
