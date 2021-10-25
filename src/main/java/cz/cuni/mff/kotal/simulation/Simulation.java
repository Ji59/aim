package cz.cuni.mff.kotal.simulation;


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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.helpers.MyGenerator.*;


/**
 * Class representing simulation.
 */
public class Simulation {
	private final SimulationGraph intersectionGraph;

	private final Map<Long, Agent> allAgents = new HashMap<>();
	private final List<Agent> delayedAgents = new ArrayList<>();
	private final Algorithm algorithm;

	private final long newAgentsMinimum;
	private final long newAgentsMaximum;
	private final List<Long> distribution;

	private long step = 0;
	private long agentsDelay;
	private long agentsRejected;
	private long collisions;

	private long startTime;
	private long period;
	private Timer timer;

	private final SimulationAgents simulationAgents;

	// TODO
	public static long MAXIMUM_DELAY = 16;


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
		newAgentsMinimum = AgentsMenuTab1.getNewAgentsMinimum().getValue();
		newAgentsMaximum = AgentsMenuTab1.getNewAgentsMaximum().getValue();
		distribution = AgentsMenuTab1.getDirectionDistribution().getChildren().stream().map(node -> ((AgentsMenuTab1.DirectionSlider) node).getValue()).collect(Collectors.toList());

		startTime = System.nanoTime();
		MAXIMUM_DELAY = intersectionGraph.getGranularity() * intersectionGraph.getEntryExitVertices().size();
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
	public Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, long newAgentsMinimum, long newAgentsMaximum, List<Long> distribution, SimulationAgents simulationAgents) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		this.simulationAgents = simulationAgents;
		step = 0;
		this.newAgentsMinimum = newAgentsMinimum;
		this.newAgentsMaximum = newAgentsMaximum;
		this.distribution = distribution;
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
		delayedAgents.clear();
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
				long stepTime = System.nanoTime();

				List<Agent> newAgents = generateAgents(allAgents.size());
				allAgents.putAll(newAgents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
				delayedAgents.addAll(newAgents);

				Map<Long, Agent> entriesAgents = new HashMap<>();
				delayedAgents.forEach(agent -> {
					if (!entriesAgents.containsKey(agent.getStart())) {
						entriesAgents.put(agent.getStart(), agent);
					}
				});

				Collection<Agent> plannedAgents = algorithm.planAgents(entriesAgents.values(), step);
				plannedAgents.forEach(agent -> simulationAgents.addAgent(stepTime, agent));

				delayedAgents.removeAll(plannedAgents);
				// TODO replace with iterator
				Set<Agent> rejectedAgents = delayedAgents.parallelStream().filter(agent -> agent.getArrivalTime() < step - MAXIMUM_DELAY).collect(Collectors.toSet());
				delayedAgents.removeAll(rejectedAgents);

				agentsRejected += rejectedAgents.size();
				agentsDelay += delayedAgents.size();

				agentsDelay += plannedAgents.stream().mapToLong(agent -> agent.getPath().size() - intersectionGraph.getLines().get(agent.getStart()).get(agent.getEnd()).size()).sum();

				IntersectionMenu.setStep(step);
				IntersectionMenu.setDelay(agentsDelay);
				IntersectionMenu.setRejections(agentsRejected);

				System.out.println(step); // TODO
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
			long entryValue = generateRandomLong(100),
				exitValue = generateRandomLong(100);

			Agent newAgent = generateAgent(id++, entryValue, exitValue, entries, exits);
			newAgents.add(newAgent);
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

		List<Vertex> directionEntries = entries.get(entryDirection);
		GraphicalVertex entry = (GraphicalVertex) directionEntries.get(generateRandomInt(directionEntries.size() - 1));
		directionEntries.remove(entry);

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
		List<Vertex> directionExits = exits.get(exitDirection);
		Vertex exit = directionExits.get(generateRandomInt(directionExits.size() - 1));

		double maxDeviation = AgentParametersMenuTab4.getSpeedDeviation().getValue();

		long minimalSpeed = AgentParametersMenuTab4.getMinimalSpeed().getValue();
		long maximalSpeed = AgentParametersMenuTab4.getMaximalSpeed().getValue();
		double speed = generateWithDeviation(minimalSpeed, maximalSpeed, maxDeviation);

		// TODO remake length and width
		long minimalLength = AgentParametersMenuTab4.getMinimalSizeLength().getValue();
		long maximalLength = AgentParametersMenuTab4.getMaximalSizeLength().getValue();
		long minimalWidth = AgentParametersMenuTab4.getMinimalSizeWidth().getValue();
		long maximalWidth = AgentParametersMenuTab4.getMaximalSizeWidth().getValue();

		double cellSize = intersectionGraph.getCellSize() * IntersectionModel.getPreferredHeight();
		// TODO extract constants
		double width = Math.max(generateWithDeviation(minimalWidth, maximalWidth, maxDeviation) - 0.7, 0.2) * cellSize;
		double length = Math.max(generateWithDeviation(minimalLength, maximalLength, maxDeviation) - 0.55, 0.3) * cellSize;

		double entryX = entry.getX() * IntersectionModel.getPreferredHeight();
		double entryY = entry.getY() * IntersectionModel.getPreferredHeight();
		double arrivalTime = step + Math.exp(-Math.random()) * maxDeviation;

		return new Agent(id, entry.getID(), exit.getID(), speed, arrivalTime, length, width, entryX, entryY);
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
