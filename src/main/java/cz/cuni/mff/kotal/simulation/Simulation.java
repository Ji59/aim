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

public abstract class Simulation {
	// TODO
	public static long maximumDelay = 16;
	protected final SimulationGraph intersectionGraph;
	protected final Map<Long, Agent> allAgents = new HashMap<>();
	protected final Algorithm algorithm;
	protected final long maximumSteps;
	protected final SimulationAgents simulationAgents;
	protected final Map<Long, List<Agent>> delayedAgents = new HashMap<>();
	protected long step = 0;
	protected long agentsDelay;
	protected long agentsRejected;
	protected long collisions;
	protected long startTime;
	protected long period;
	protected State state;

	protected Simulation() {
		state = State.INVALID;
		intersectionGraph = null;
		algorithm = null;
		maximumSteps = 0;
		simulationAgents = null;
	}

	protected Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		this.simulationAgents = simulationAgents;
		this.maximumSteps = AgentsMenuTab1.getSteps();

		state = State.STOPPED;

		startTime = System.nanoTime();
		maximumDelay = intersectionGraph.getGranularity() * intersectionGraph.getEntryExitVertices().size();

		intersectionGraph.getEntryExitVertices().values().forEach(
			directionList -> directionList.stream()
				.filter(vertex -> vertex.getType().isEntry())
				.forEach(entry -> delayedAgents.put(entry.getID(), new ArrayList<>()))
		);
	}

	protected Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, long maximumSteps, SimulationAgents simulationAgents) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		this.maximumSteps = maximumSteps;
		this.simulationAgents = simulationAgents;

		startTime = System.nanoTime();

		state = State.STOPPED;
	}

	/**
	 * TODO
	 *
	 * @param step
	 * @return
	 */
	public abstract Set<Agent> loadAgents(double step);

	/**
	 * TODO
	 *
	 * @param period Time in nanoseconds
	 */
	public final void start(long period) {
		this.period = period;
		if (!isRunning()) {
			IntersectionMenu.setAgents(0);
			IntersectionMenu.setStep(0);
			IntersectionMenu.setDelay(0);
			IntersectionMenu.setCollisions(0);
		}
		state = State.RUNNING;

		start();
		simulationAgents.resumeSimulation(this, startTime);
	}

	protected abstract void start();

	public final void stop() {
		if (state == State.RUNNING) {
			state = State.STOPPED;
			simulationAgents.pauseSimulation();
			stopSimulation();
		}
	}

	protected void updateStatistics(long agents) {
		IntersectionMenu.setAgents(agents);
		IntersectionMenu.setDelay(agentsDelay);
		IntersectionMenu.setRejections(agentsRejected);
	}

	/**
	 * TODO
	 * @param step
	 */
	protected abstract void updateAgentsDelay(double step);

	/**
	 * TODO
	 * @param agent
	 * @return
	 */
	protected int getAgentsDelay(Agent agent) {
		return agent.getPath().size() - getIntersectionGraph().getLines().get(agent.getEntry()).get(agent.getPath().get(agent.getPath().size() - 1)).size();
	}

	protected abstract void updateRejectedAgents(double step);

	protected abstract void stopSimulation();

	public final void reset() {
		state = State.INVALID;
		delayedAgents.values().forEach(Collection::clear);
		resetSimulation();
	}

	protected abstract void resetSimulation();

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
		return (time - startTime) / (double) period;
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
