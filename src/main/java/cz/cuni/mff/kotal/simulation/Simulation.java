package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.SquareGraph;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


/**
 * TODO
 */
public abstract class Simulation {
	public static final int GENERATED_MINIMUM_STEP_AHEAD = 8;
	public static final int GENERATED_MAXIMUM_STEP_AHEAD = 16;
	// TODO
	public static long maximumDelay = 16;
	protected final SimulationGraph intersectionGraph;
	protected final SortedMap<Long, Agent> allAgents = new TreeMap<>();
	protected final @Nullable Algorithm algorithm;
	protected final @Nullable SimulationAgents simulationAgents;
	protected final Map<Integer, List<Agent>> delayedAgents = new HashMap<>();
	protected final PriorityQueue<Agent> createdAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getArrivalTime));
	protected final PriorityQueue<Agent> plannedAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getPlannedTime));
	protected final PriorityQueue<Agent> rejectedAgentsQueue = new PriorityQueue<>(Comparator.comparingDouble(Agent::getArrivalTime));
	protected final List<Long> planningTime = new LinkedList<>();
	protected final Lock stateLock = new ReentrantLock();
	private final Lock loadAgentsLock = new ReentrantLock(false);
	protected long step = 0;
	protected long agentsTotal = 0;
	protected long agentsDelay = 0;
	protected long agentsRejected = 0;
	protected long collisions;
	@Deprecated
	protected long startTime;
	protected double startingStep = 0;
	protected long period;
	protected State state;
	protected long loadedStep = 0;
	protected long delayedStep = 0;
	protected boolean ended = false;
	protected long finalStep = 0;
	protected long remainingTime = Long.MAX_VALUE;
	protected Thread timeoutThread = null;

	protected Simulation() {
		stateLock.lock();
		state = State.INVALID;
		intersectionGraph = new SquareGraph(4, 1, 1);
		algorithm = null;
		simulationAgents = null;
		stateLock.unlock();
	}

	protected Simulation(@NotNull SimulationGraph intersectionGraph, @NotNull Algorithm algorithm, @NotNull SimulationAgents simulationAgents) {
		stateLock.lock();
		this.intersectionGraph = intersectionGraph;
		this.algorithm = algorithm;
		this.simulationAgents = simulationAgents;

		state = State.STOPPED;

		maximumDelay = (long) intersectionGraph.getGranularity() * intersectionGraph.getEntryExitVertices().size();

		intersectionGraph.getEntryExitVertices().values().forEach(
			directionList -> directionList.stream()
				.filter(vertex -> vertex.getType().isEntry())
				.forEach(entry -> delayedAgents.put(entry.getID(), new ArrayList<>()))
		);

		long stopAfter = Long.parseLong(IntersectionMenu.getTimeout().getText());
		if (stopAfter > 0) {
			remainingTime = stopAfter * 1_000;
		}

		stateLock.unlock();
	}

	private Thread createTimeoutThread() {
		return new Thread(() -> {
			long start = System.currentTimeMillis();
			try {
				Thread.sleep(remainingTime);
				ended = true;
			} catch (InterruptedException e) {
				remainingTime -= System.currentTimeMillis() - start;
			}
		});
	}

	/**
	 * TODO
	 *
	 * @param period Time in nanoseconds
	 */
	public final void start(long period) {
		stateLock.lock();
		IntersectionMenu.getTimeout().setDisable(true);
		state = State.RUNNING;
		this.period = period;

		new Thread(() -> {
			if (startingStep <= 0) {
				loadAndUpdateAgents(0.);
			}
			simulationAgents.resumeSimulation(this);
		}).start();

		timeoutThread = createTimeoutThread();
		timeoutThread.start();

		if (startingStep <= 0) {
			IntersectionMenu.setAgents(0);
			IntersectionMenu.setStep(0);
			IntersectionMenu.setDelay(0);
			IntersectionMenu.setCollisions(0);
		}
		stateLock.unlock();
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	public void loadAndUpdateAgents(double step) {
		final double stepDiff = loadedStep - step;

		if (stepDiff <= GENERATED_MINIMUM_STEP_AHEAD) {
			final @NotNull Thread thread = new Thread(() -> {
				loadAgentsLock.lock();
				try {
					for (; loadedStep <= step + GENERATED_MAXIMUM_STEP_AHEAD && isRunning(); loadedStep++) {
						if (ended) {
							continue;
						}
						try {
							loadAndUpdateAgents(loadedStep);
						} catch (Exception e) {
							stop();
							Platform.runLater(() -> {
								final @NotNull Alert alert = new Alert(Alert.AlertType.ERROR);
								alert.setTitle("Algorithm planning failed");
								alert.setHeaderText("Exception occurred during computing algorithm plan at step " + step);
								alert.setContentText(e.getMessage());
								alert.showAndWait();
							});
							return;
						}
					}
				} finally {
					loadAgentsLock.unlock();
				}
			});
			thread.start();

			if (stepDiff < GENERATED_MINIMUM_STEP_AHEAD && !Platform.isFxApplicationThread()) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
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

	protected void addCreatedAgents(@NotNull Collection<Agent> newAgents) {
		synchronized (delayedAgents) {
			newAgents.forEach(agent -> delayedAgents.get(agent.getEntry()).add(agent));
		}

		synchronized (createdAgentsQueue) {
			createdAgentsQueue.addAll(newAgents);
		}
	}

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
		final long startTime = System.nanoTime();
		final Collection<Agent> plannedAgents = algorithm.planAgents(entriesAgents, step);
		final long endTime = System.nanoTime();
		planningTime.add(endTime - startTime);

		plannedAgents.forEach(agent -> {
			assert !agent.getPath().isEmpty() && agent.getPlannedTime() >= 0;
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

	protected boolean isRejected(double step, @NotNull Agent rejectedAgent) {
		return rejectedAgent.getArrivalTime() + maximumDelay <= step;
	}

	public final void stop() {
		stateLock.lock();
		if (state == State.RUNNING) {
			timeoutThread.interrupt();
			state = State.STOPPED;
			simulationAgents.pauseSimulation();
			IntersectionMenu.setPlayButtonPlaying(false);
		}
		stateLock.unlock();
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
		simulationAgents.resumeSimulation(this);
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	public final void setStartAndAgents(double step) {
		simulationAgents.setAgents(allAgents.values(), step);
		startingStep = step;
	}

	protected abstract void start();

	@Deprecated
	protected abstract void stopSimulation();

	public final void reset() {
		assert simulationAgents != null;
		simulationAgents.pauseSimulation();
		assert algorithm != null;
		algorithm.stop();

		stateLock.lock();
		state = State.INVALID;
		timeoutThread.interrupt();
		IntersectionMenu.getTimeout().setDisable(false);
		resetSimulation();
		synchronized (delayedAgents) {
			delayedAgents.values().forEach(Collection::clear);
		}
		stateLock.unlock();
	}

	protected abstract void resetSimulation();

	@Deprecated
	protected void updateStatistics(long agents) {
		IntersectionMenu.setAgents(agents);
		IntersectionMenu.setDelay(agentsDelay);
		IntersectionMenu.setRejections(agentsRejected);
	}

	public void updateStatistics(double step) {
		updateAgentsStats(step);

		IntersectionMenu.setAgentsDelayRejections(agentsTotal, agentsDelay, agentsRejected);
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
	 * @param step
	 */
	protected void updateTotalAgents(double step) {
		synchronized (createdAgentsQueue) {
			@NotNull Iterator<Agent> iterator = createdAgentsQueue.iterator();
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

			@NotNull Iterator<Agent> iterator = plannedAgentsQueue.iterator();
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
	 * @param agent
	 * @return
	 */
	protected double getAgentsDelay(@NotNull Agent agent) {
		final int exitID = agent.getPath().get(agent.getPath().size() - 1);
		return agent.getPath().size() - getIntersectionGraph().getDistance(agent.getEntry(), exitID);
	}

	/**
	 * @return Graph of this simulation
	 */
	public SimulationGraph getIntersectionGraph() {
		return intersectionGraph;
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	protected void updateRejectedAgents(double step, long stepRounded) {
		synchronized (rejectedAgentsQueue) {
			@NotNull Iterator<Agent> iterator = rejectedAgentsQueue.iterator();
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

	/**
	 * @return If this simulation is running
	 */
	public boolean isRunning() {
		return state == State.RUNNING;
	}

	/**
	 * @return All generated agents
	 */
	public @NotNull Collection<Agent> getAllAgents() {
		return allAgents.values();
	}

	public @NotNull Collection<Agent> getAllCreatedAgents() {
		return allAgents.values().stream().filter(agent -> !createdAgentsQueue.contains(agent)).toList();
	}

	/**
	 * TODO
	 *
	 * @return
	 */
	public @NotNull Map<Long, Agent> getAllAgentsMap() {
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

	public Algorithm getAlgorithm() {
		return algorithm;
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
		return time / (double) period + startingStep;
	}

	/**
	 * TODO
	 *
	 * @return
	 */
	public double getNextStep() {
		return startingStep;
	}

	public double getStartingStep() {
		return startingStep;
	}

	public @NotNull Simulation setStartingStep(double startingStep) {
		this.startingStep = startingStep;
		return this;
	}

	/**
	 * TODO
	 *
	 * @param step
	 * @return
	 */
	@Deprecated
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
	@Deprecated
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

	public @NotNull Lock getStateLock() {
		return stateLock;
	}

	public long getLoadedStep() {
		return loadedStep;
	}

	/**
	 * TODO
	 *
	 * @return
	 */
	public boolean ended() {
		return ended;
	}

	public long getAgents() {
		return agentsTotal;
	}

	public long getDelay() {
		return agentsDelay;
	}

	public long getRejections() {
		return agentsRejected;
	}

	public long getCollisions() {
		return collisions;
	}

	public @NotNull List<Long> getPlanningTime() {
		return planningTime;
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
