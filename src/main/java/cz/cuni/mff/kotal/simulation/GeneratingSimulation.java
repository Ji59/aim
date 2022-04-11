package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentParametersMenuTab4;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentsMenuTab1;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.helpers.MyGenerator.*;
import static cz.cuni.mff.kotal.helpers.MyNumberOperations.myModulo;


/**
 * Class representing simulation.
 */
public class GeneratingSimulation extends Simulation {

	private static final boolean randomEntry = false;
	private static final boolean generateEntry = true;
	private static final boolean generateExit = false; // TODO

	protected final long newAgentsMinimum;
	protected final long newAgentsMaximum;
	protected final List<Long> distribution;

	private long finalStep = 0;

	private Timer timer;


	/**
	 * Create new simulation.
	 *
	 * @param intersectionGraph Graph of the simulation
	 * @param algorithm         Algorithm used in computation
	 * @param simulationAgents  Agents simulation pane
	 */
	public GeneratingSimulation(SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents) {
		super(intersectionGraph, algorithm, simulationAgents);
		maximumDelay = intersectionGraph.getGranularity() * intersectionGraph.getEntryExitVertices().size();


		newAgentsMinimum = AgentsMenuTab1.getNewAgentsMinimum().getValue();
		distribution = AgentsMenuTab1.getDirectionDistribution().getChildren().stream().map(node -> ((AgentsMenuTab1.DirectionSlider) node).getValue()).collect(Collectors.toList());

//		newAgentsMaximum = AgentsMenuTab1.getNewAgentsMaximum().getValue(); TODO
		newAgentsMaximum = Math.min(AgentsMenuTab1.getNewAgentsMaximum().getValue(), getDistribution().stream().filter(d -> d > 0).count());
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
	public GeneratingSimulation(SimulationGraph intersectionGraph, Algorithm algorithm, long maximumSteps, long newAgentsMinimum, long newAgentsMaximum, List<Long> distribution, SimulationAgents simulationAgents) {
		super(intersectionGraph, algorithm, maximumSteps, simulationAgents);

		this.newAgentsMinimum = newAgentsMinimum;
		this.distribution = distribution;

		this.newAgentsMaximum = Math.min(newAgentsMaximum, distribution.stream().filter(d -> d > 0).count());
	}


	/**
	 * Start this simulation.
	 * Create timer for agents generation and planning.
	 * FIXME
	 */
	@Override
	public void start() {
		long msPeriod = period / 1_000_000;
		long delay = isRunning() ? msPeriod : 0L;
		startTime = System.nanoTime() - (isRunning() ? period : 0);

		timer = new Timer();
		timer.scheduleAtFixedRate(getTimerTask(), delay, msPeriod); // TODO refactor delay
		new Thread(() -> {
			try {
				// FIXME WTH?
				long now = System.nanoTime();
				Thread.sleep(delay);
				long noNow = System.nanoTime();
				System.out.println((noNow - now) / 1_000_000 + "; " + delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * Stop this simulation.
	 */
	@Override
	public void stopSimulation() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * Reset this simulation.
	 */
	@Override
	public void resetSimulation() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * TODO
	 *
	 * @param step
	 * @return True if simulation has finished else false
	 */
	@Override
	protected boolean loadAndUpdateStepAgents(long step) {
		if (maximumSteps > 0 && step > maximumSteps) {
			synchronized (delayedAgents) {
				if (delayedAgents.values().stream().allMatch(Collection::isEmpty)) {
					// FIXME refactor
					if (step > finalStep) {
						ended = true;
					} else {
						updateStatistics(allAgents.size());
					}
					return true;
				}
			}
		} else {
			List<Agent> newAgents = generateAgents(step, allAgents.size());
			allAgents.putAll(newAgents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
			synchronized (delayedAgents) {
				newAgents.forEach(agent -> delayedAgents.get(agent.getEntry()).add(agent));
			}

			synchronized (createdAgentsQueue) {
				createdAgentsQueue.addAll(newAgents);
			}
		}

		List<Agent> entriesAgents;
		synchronized (delayedAgents) {
			entriesAgents = delayedAgents.values().stream().map(entryList -> entryList.stream().findFirst().orElse(null)).filter(Objects::nonNull).toList();
		}

		assert algorithm != null;
		Collection<Agent> plannedAgents = algorithm.planAgents(entriesAgents, step);
		plannedAgents.forEach(agent -> {
			agent.setPlannedTime(step);
			assert simulationAgents != null;
			simulationAgents.addAgent(agent);

			long leavingTime = agent.getPath().size() + step;
			if (leavingTime > finalStep) {
				finalStep = leavingTime;
			}
		});
		synchronized (plannedAgentsQueue) {
			plannedAgentsQueue.addAll(plannedAgents);
		}

		Set<Agent> rejectedAgents;
		synchronized (delayedAgents) {
			delayedAgents.values().forEach(entryList -> entryList.removeAll(plannedAgents));

			// TODO replace with iterator
			rejectedAgents = delayedAgents.values().stream().flatMap(Collection::stream).filter(agent -> agent.getArrivalTime() + maximumDelay <= step).collect(Collectors.toSet());
			delayedAgents.values().forEach(entryList -> entryList.removeAll(rejectedAgents));
		}

		synchronized (rejectedAgentsQueue) {
			rejectedAgentsQueue.addAll(rejectedAgents);
		}

		return false;
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
				long currentStep = getStep(); // TODO

				loadAndUpdateStepAgents(currentStep);


				updateTotalAgents(currentStep);
				updateAgentsDelay(currentStep);
				updateRejectedAgents(currentStep);
				updateStatistics(allAgents.size());
				step++;
			}
		};
	}

	/**
	 * Generate new agents with IDs starting from specified ID.
	 *
	 * @param id ID of first generated agent; increases with reach generated agent
	 * @return Set of newly generated agents
	 */
	protected List<Agent> generateAgents(long step, int id) {
		assert (distribution.stream().reduce(0L, Long::sum) == 100);
		long newAgentsCount = generateRandomLong(newAgentsMinimum, newAgentsMaximum);

		List<Agent> newAgents = new ArrayList<>();
		Map<Integer, List<Vertex>> entries = getEntriesExitsMap(true);
		Map<Integer, List<Vertex>> exits = getEntriesExitsMap(false);

		for (long i = 0; i < newAgentsCount; i++) {
			long entryValue = generateRandomLong(100);
			long exitValue = generateRandomLong(100);

			Agent newAgent = generateAgent(step, id++, entryValue, exitValue, entries, exits);
			newAgents.add(newAgent);
		}

		if (!randomEntry && generateEntry) {
			Map<Long, List<Agent>> directionsAgents = new HashMap<>();
			int directions = getIntersectionGraph().getEntryExitVertices().size();
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
				List<Vertex> directionEntries = getIntersectionGraph().getEntryExitVertices().get(entryDirection).stream().filter(vertex -> vertex.getType().isEntry()).toList(); // FIXME parallelStream?

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
	protected Agent generateAgent(double step, int id, long entryValue, long exitValue, Map<Integer, List<Vertex>> entries, Map<Integer, List<Vertex>> exits) {
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
		entry = (GraphicalVertex) directionEntries.get(generateRandomInt(directionEntries.size() - 1));
		directionEntries.remove(entry);
		entryX = entry.getX() * IntersectionModel.getPreferredHeight();
		entryY = entry.getY() * IntersectionModel.getPreferredHeight();

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
		for (Map.Entry<Integer, List<Vertex>> entry : getIntersectionGraph().getEntryExitVertices().entrySet()) {
			entries.put(entry.getKey(), entry.getValue().stream().filter(v -> isEntry == v.getType().isEntry()).collect(Collectors.toCollection(LinkedList::new)));
		}
		return entries;
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

}
