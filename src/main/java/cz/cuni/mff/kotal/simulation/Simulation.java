package cz.cuni.mff.kotal.simulation;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.SquareGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


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
	protected final SimulationAgents simulationAgents;
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
	protected final Map<Long, List<Agent>> delayedAgents = new HashMap<>();
	protected final PriorityQueue<Agent> createdAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getArrivalTime));
	protected final PriorityQueue<Agent> plannedAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getPlannedTime));
	protected final PriorityQueue<Agent> rejectedAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getArrivalTime));

	private final Lock loadAgentsLock = new ReentrantLock(false);

	protected boolean ended = false;
	protected long finalStep = 0;

	protected Simulation() {
		state = State.INVALID;
		intersectionGraph = new SquareGraph(4, 1, 1);
		algorithm = null;
		simulationAgents = null;
	}

	protected Simulation(SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents) {
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
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
	public void loadAndUpdateAgents(double step) {
		double stepDiff = loadedStep - step;

		/**
		 if (loadedStep <= step + GENERATED_MINIMUM_STEP_AHEAD) {
		 loadAgentsLock.lock();
		 for (; loadedStep <= step + GENERATED_MAXIMUM_STEP_AHEAD; loadedStep++) {
		 updateRejectedAgents(loadedStep);
		 Collection<Agent> newAgents = loadAgents(loadedStep);
		 addCreatedAgents(newAgents);
		 updateDelayedAgents(loadedStep);
		 }
		 loadAgentsLock.unlock();
		 }

		 /*/

		if (stepDiff <= GENERATED_MINIMUM_STEP_AHEAD / 2) {
			try {
				loadAgentsLock.lock();
				for (; loadedStep - step <= GENERATED_MAXIMUM_STEP_AHEAD; loadedStep++) {
					loadAndUpdateAgents(loadedStep);
				}
			} finally {
				loadAgentsLock.unlock();
			}
		}

		if (stepDiff <= GENERATED_MINIMUM_STEP_AHEAD) {
			new Thread(() -> {
				loadAgentsLock.lock();
				try {
					for (; loadedStep <= step + GENERATED_MAXIMUM_STEP_AHEAD; loadedStep++) {
						loadAndUpdateAgents(loadedStep);
					}
				} finally {
					loadAgentsLock.unlock();
				}
			}).start();
		}
		/**/
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	private void loadAndUpdateAgents(long step) {
		updateRejectedAgents(step);
		Collection<Agent> newAgents = loadAgents(step);
		addCreatedAgents(newAgents);
		updateDelayedAgents(step);
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
			loadAndUpdateAgents(0.);
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
	 *
	 * @param step
	 */
	public final void setStartAndAgents(double step) {
		startingStep = step;
		simulationAgents.setAgents(allAgents.values(), step);
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

	protected void addCreatedAgents(Collection<Agent> newAgents) {
		synchronized (delayedAgents) {
			newAgents.forEach(agent -> delayedAgents.get(agent.getEntry()).add(agent));
		}

		synchronized (createdAgentsQueue) {
			createdAgentsQueue.addAll(newAgents);
		}
	}

	protected abstract void stopSimulation();

	public final void reset() {
		state = State.INVALID;
		delayedAgents.values().forEach(Collection::clear);
		resetSimulation();
	}

	protected abstract void resetSimulation();

	/**
	 * TODO
	 *
	 * @param step
	 */
	protected abstract Collection<Agent> loadAgents(long step);

	protected void updateDelayedAgents(long step) {
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

		synchronized (delayedAgents) {
			delayedAgents.values().forEach(entryList -> entryList.removeAll(plannedAgents));
		}
	}

	private void updateRejectedAgents(long step) {
		Set<Agent> rejectedAgents;
		synchronized (delayedAgents) {
			// TODO replace with iterator
			rejectedAgents = delayedAgents.values().parallelStream().flatMap(Collection::stream).filter(agent -> isRejected(step, agent)).collect(Collectors.toSet());
			delayedAgents.values().forEach(entryList -> entryList.removeAll(rejectedAgents));
		}

		synchronized (rejectedAgentsQueue) {
			rejectedAgentsQueue.addAll(rejectedAgents);
		}
	}

	@Deprecated
	protected void updateStatistics(long agents) {
		IntersectionMenu.setAgents(agents);
		IntersectionMenu.setDelay(agentsDelay);
		IntersectionMenu.setRejections(agentsRejected);
	}

	public void updateStatistics(double step) {
		updateAgentsStats(step);

		assert agentsTotal == allAgents.values().stream().filter(agent -> agent.getArrivalTime() <= step).count();
		IntersectionMenu.setAgents(agentsTotal);
		IntersectionMenu.setDelay(agentsDelay);
		IntersectionMenu.setRejections(agentsRejected);
	}

	protected void updateAgentsStats(double step) {
		final long step_rounded = (long) step + 1;

		// TODO maybe compute delay from actualAgentsQueue

		updateTotalAgents(step);
		updateAgentsDelay(step_rounded);
		updateRejectedAgents(step, step_rounded);

		delayedStep = step_rounded;
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
		synchronized (createdAgentsQueue) {
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
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	protected void updateAgentsDelay(long step) {
		synchronized (delayedAgents) {
			agentsDelay += delayedAgents.values().parallelStream().flatMap(Collection::stream).mapToLong(agent -> Math.max(0, step - Math.max((long) agent.getArrivalTime(), delayedStep))).sum();
		}

		synchronized (plannedAgentsQueue) {
			agentsDelay += plannedAgentsQueue.stream().filter(agent -> agent.getArrivalTime() <= step).mapToLong(agent -> Math.min(step, agent.getPlannedTime()) - Math.max((long) agent.getArrivalTime(), delayedStep)).sum();

			Iterator<Agent> iterator = plannedAgentsQueue.iterator();
			while (iterator.hasNext()) {
				Agent agent = iterator.next();
				if (agent.getPlannedTime() > delayedStep) {
					return;
				}
				agentsDelay += getAgentsDelay(agent);
				iterator.remove();
			}
		}
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	protected void updateRejectedAgents(double step, long stepRounded) {
		synchronized (rejectedAgentsQueue) {
			Iterator<Agent> iterator = rejectedAgentsQueue.iterator();
			while (iterator.hasNext()) {
				Agent rejectedAgent = iterator.next();
				if (rejectedAgent.getArrivalTime() >= delayedStep) {
					return;
				} else if (isRejected(step, rejectedAgent)) {
					agentsRejected++;
					iterator.remove();
				} else {
					agentsDelay += stepRounded - Math.max((long) rejectedAgent.getArrivalTime(), delayedStep);
				}
			}
		}
	}

	protected boolean isRejected(double step, Agent rejectedAgent) {
		return rejectedAgent.getArrivalTime() + maximumDelay <= step;
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
	@Deprecated
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
	 * @return
	 */
	public boolean ended() {
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
