package cz.cuni.mff.kotal.simulation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentsMenuTab1;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * TODO
 */
public abstract class Simulation {
	protected static final double GENERATED_MINIMUM_STEP_AHEAD = 8;
	protected static final double GENERATED_MAXIMUM_STEP_AHEAD = 16;
	// TODO
	public static long maximumDelay = 16;
	protected final SimulationGraph intersectionGraph;
	protected final SortedMap<Long, Agent> allAgents = new TreeMap<>();
	protected final Algorithm algorithm;
	protected final long maximumSteps;
	protected final SimulationAgents simulationAgents;
	protected final Map<Long, List<Agent>> delayedAgents = new HashMap<>();
	protected long step = 0;
	protected long agentsTotal = 0;
	protected long agentsDelay = 0;
	protected long agentsRejected = 0;
	protected long collisions;
	protected long startTime;
	protected double startingStep = 0;
	protected long period;
	protected State state;


	protected long loadedStep = 0;
	protected long delayedStep = 0;
	protected PriorityQueue<Agent> createdAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getArrivalTime));
	protected PriorityQueue<Agent> plannedAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getPlannedTime));
	protected PriorityQueue<Agent> rejectedAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getArrivalTime));

	protected boolean ended = false;

	protected Simulation() {
		state = State.INVALID;
		intersectionGraph = null;
		algorithm = null;
		maximumSteps = 0;
		simulationAgents = null;
	}

	protected Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents) {
		this(intersectionGraph, algorithm, AgentsMenuTab1.getSteps(), simulationAgents);
	}

	protected Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, long maximumSteps, SimulationAgents simulationAgents) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		this.maximumSteps = maximumSteps;
		this.simulationAgents = simulationAgents;

		state = State.STOPPED;

		maximumDelay = intersectionGraph.getGranularity() * intersectionGraph.getEntryExitVertices().size();

		intersectionGraph.getEntryExitVertices().values().forEach(
			directionList -> directionList.stream()
				.filter(vertex -> vertex.getType().isEntry())
				.forEach(entry -> delayedAgents.put(entry.getID(), new ArrayList<>()))
		);
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	public void loadAgents(double step) {
		if (loadedStep <= step + GENERATED_MINIMUM_STEP_AHEAD) {
			new Thread(() -> {
				for (; loadedStep <= step + GENERATED_MAXIMUM_STEP_AHEAD; loadedStep++) {
					loadAndUpdateStepAgents(loadedStep);
				}
			}).start();
		}
	}

	/**
	 * TODO
	 *
	 * @param period Time in nanoseconds
	 */
	public final void start(long period) {
		if (ended) {
			return;
		}

		this.period = period;
		if (startingStep <= 0) {
			loadAgents(0);
			IntersectionMenu.setAgents(0);
			IntersectionMenu.setStep(0);
			IntersectionMenu.setDelay(0);
			IntersectionMenu.setCollisions(0);
		}
		state = State.RUNNING;

//		start();
		startTime = System.nanoTime();
		simulationAgents.resumeSimulation(this, startTime);
	}

	/**
	 * TODO
	 *
	 * @param period Time in nanoseconds
	 */
	@Deprecated
	public final void startAt(long period, double step) {
		if (ended) {
			return;
		}

		this.period = period;
		state = State.RUNNING;
		setStartAndAgents(step);
//		simulationAgents.resumeSimulationWithAgents(this, startTime, allAgents.values());
		startTime = System.nanoTime();
		simulationAgents.resumeSimulation(this, startTime);
	}

	/**
	 * TODO
	 * @param step
	 */
	public final void setStartAndAgents(double step) {
		startingStep = step;
		simulationAgents.setAgents(allAgents.values(),  step);
	}

	protected abstract void start();

	public final void stop() {
		if (state == State.RUNNING) {
			state = State.STOPPED;
			simulationAgents.pauseSimulation();
			startingStep = getStep(System.nanoTime());
//			stopSimulation();
		}
	}

	protected abstract void stopSimulation();

	public final void reset() {
		state = State.INVALID;
		delayedAgents.values().forEach(Collection::clear);
		resetSimulation();
	}

	protected abstract void resetSimulation();

	protected void updateStatistics(long agents) {
		IntersectionMenu.setAgents(agents);
		IntersectionMenu.setDelay(agentsDelay);
		IntersectionMenu.setRejections(agentsRejected);
	}

	public void updateStatistics(double step) {
		updateTotalAgents(step);
		updateAgentsDelay(step);
		updateRejectedAgents(step);

		assert agentsTotal == allAgents.values().stream().filter(agent -> agent.getArrivalTime() <= step).count();
		IntersectionMenu.setAgents(agentsTotal);
		IntersectionMenu.setDelay(agentsDelay);
		IntersectionMenu.setRejections(agentsRejected);
	}

	/**
	 * TODO
	 *
	 * @param agent
	 * @return
	 */
	protected int getAgentsDelay(Agent agent) {
		return agent.getPath().size() - getIntersectionGraph().getLines().get(agent.getEntry()).get(agent.getPath().get(agent.getPath().size() - 1)).size();
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	protected void updateTotalAgents(double step) {
		Iterator<Agent> iterator = createdAgentsQueue.iterator();
		while (iterator.hasNext()) {
			Agent createdAgent = iterator.next();
			if (createdAgent.getArrivalTime() > step) {
				return;
			}

			agentsTotal++;
			iterator.remove();
		}
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	protected void updateAgentsDelay(double step) {
		if (step > delayedStep) {
			agentsDelay += delayedAgents.values().stream().flatMap(Collection::stream).filter(agent -> agent.getArrivalTime() <= step).count(); // FIXME use paralel stream?

			delayedStep++;
		}

		Iterator<Agent> iterator = plannedAgentsQueue.iterator();
		while (iterator.hasNext()) {
			Agent agent = iterator.next();
			if (agent.getPlannedTime() > step) {
				return;
			}

			agentsDelay += getAgentsDelay(agent);
			iterator.remove();
		}
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	protected void updateRejectedAgents(double step) {
		Iterator<Agent> iterator = rejectedAgentsQueue.iterator();
		while (iterator.hasNext()) {
			Agent rejectedAgent = iterator.next();
			if (isRejected(step, rejectedAgent)) {
				return;
			}

			agentsRejected++;
			iterator.remove();
		}
	}

	protected boolean isRejected(double step, Agent rejectedAgent) {
		return rejectedAgent.getArrivalTime() + maximumDelay > step;
	}

	public void saveAgents(File file) throws IOException {
		FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8, false);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		gson.toJson(allAgents.values(), writer);
		writer.close();
	}

	/**
	 * @return If this simulation is running
	 */
	public boolean isRunning() {
		return state == State.RUNNING;
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
	 * TODO
	 *
	 * @return
	 */
	public Map<Long, Agent> getAllAgentsMap() {
		return allAgents;
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
	 * @return Actual simulation step
	 */
	public long getStep() {
		return step;
	}

	/**
	 * TODO
	 *
	 * @param time
	 * @return
	 */
	public double getStep(long time) {
		return (time - startTime) / (double) period + startingStep;
	}

	/**
	 * TODO
	 *
	 * @param step
	 * @return
	 */
	public long getTime(long step) {
		return step * period + startTime;
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

	public State getState() {
		return state;
	}

	/**
	 * TODO
	 *
	 * @param step
	 * @return
	 */
	protected abstract boolean loadAndUpdateStepAgents(long step);

	/**
	 * TODO
	 *
	 * @return
	 */
	public boolean isEnded() {
		return ended;
	}

	public enum State {
		RUNNING(true),
		STOPPED(true),
		INVALID(false),
		;

		private final boolean valid;

		State(boolean valid) {
			this.valid = valid;
		}

		public boolean isValid() {
			return valid;
		}
	}
}
