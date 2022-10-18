package cz.cuni.mff.kotal.backend.algorithm.cbs;

import cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Quaternion;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirection.Distance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CBSSingleGrouped extends AStarSingle {
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(AStarSingle.PARAMETERS);

	protected static final String SIMPLE_STRATEGY_NAME = "Simple strategy after (ms)";
	protected static final long SIMPLE_STRATEGY_DEF = 1000;

	static {
		PARAMETERS.put(SIMPLE_STRATEGY_NAME, SIMPLE_STRATEGY_DEF);
	}

	protected final Map<Agent, List<Integer>> initialPaths = new HashMap<>();
	protected final long simpleStrategyAfter;
	protected final ReentrantLock simpleLock = new ReentrantLock();
	protected boolean simple = false;

	public CBSSingleGrouped(SimulationGraph graph) {
		super(graph);
		simpleStrategyAfter = AlgorithmMenuTab2.getLongParameter(SIMPLE_STRATEGY_NAME, SIMPLE_STRATEGY_DEF);
	}

	@TestOnly
	protected CBSSingleGrouped(SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn, long simpleStrategyAfter) {
		super(graph, safeDistance, maximumVertexVisits, allowAgentStop, maximumPathDelay, allowAgentReturn);
		this.simpleStrategyAfter = simpleStrategyAfter;
	}

	@NotNull
	private static Quaternion<Agent, Agent, Long, Boolean> switchCollisionAgents(@NotNull Quaternion<Agent, Agent, Long, Boolean> collidingAgents) {
		return new Quaternion<>(collidingAgents.getVal1(), collidingAgents.getVal0(), collidingAgents.getVal2(), collidingAgents.getVal3());
	}

	@NotNull
	public static Pair<Integer, Integer> createCollision(@NotNull Agent agent, @NotNull Quaternion<Agent, Agent, Long, Boolean> collision) {
		final long collisionStep = collision.getVal2();
		final boolean collisionOnTravel = collision.getVal3();

		final int collisionRelativeTime = (int) (collisionStep - agent.getPlannedTime());
		final List<Integer> agentPath = agent.getPath();
		final int collisionStepVertex = agentPath.get(collisionRelativeTime);

		return new Pair<>(collisionOnTravel ? collisionStepVertex : agentPath.get(collisionRelativeTime - 1), collisionStepVertex);
	}

	@NotNull
	protected static Collection<Agent> copyAgents(long step, @NotNull Node node, @NotNull Agent agent, List<Integer> path) {
		Collection<Agent> agents;
		final Agent agentCopy = new Agent(agent).setPath(path, step);
		agents = new HashSet<>(node.getAgents().size());
		for (Agent nodeAgent : node.getAgents()) {
			if (agentCopy.equals(nodeAgent)) {
				agents.add(agentCopy);
			} else {
				agents.add(nodeAgent);
			}
		}
		return agents;
	}

	protected static void removeAgentFromConstraints(final @NotNull Node parentNode, final @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, final Agent agent) {
		constraints.remove(agent);

		final @NotNull Set<Agent> changedAgents = new HashSet<>(parentNode.getAgents().size());

		for (@NotNull Node parent = parentNode; parent.getCollision() != null; parent = Objects.requireNonNull(parent.getParent())) {
			final Quaternion<Agent, Agent, Long, Boolean> parentCollision = parent.getCollision();
			Agent otherAgent;
			if (parentCollision.getVal0().equals(agent)) {
				otherAgent = parentCollision.getVal1();
			} else if (parentCollision.getVal1().equals(agent)) {
				otherAgent = parentCollision.getVal0();
			} else {
				continue;
			}

			if (parentNode.agents.contains(otherAgent) && parent.agents.stream().noneMatch(a -> a == otherAgent)) {
				Map<Long, Collection<Pair<Integer, Integer>>> otherAgentConstraints = constraints.get(otherAgent);
				final long collisionStep = parentCollision.getVal2();

				final Collection<Pair<Integer, Integer>> stepConstraints = otherAgentConstraints.get(collisionStep);
				final @NotNull Pair<Integer, Integer> collisionConstraint = createCollision(otherAgent, parentCollision);
				if (stepConstraints.contains(collisionConstraint)) {
					final @NotNull Set<Pair<Integer, Integer>> stepConstraintsCopy = new HashSet<>(stepConstraints.size());
					for (final @NotNull Pair<Integer, Integer> constraint : stepConstraints) {
						if (!constraint.equals(collisionConstraint)) {
							stepConstraintsCopy.add(constraint);
						}
					}

					if (!changedAgents.contains(otherAgent)) {
						changedAgents.add(otherAgent);
						final @NotNull Map<Long, Collection<Pair<Integer, Integer>>> otherAgentConstraintsCopy = new HashMap<>(otherAgentConstraints.size());
						for (Map.@NotNull Entry<Long, Collection<Pair<Integer, Integer>>> stepConstraintsEntry : otherAgentConstraints.entrySet()) {
							final long step = stepConstraintsEntry.getKey();
							if (step != collisionStep) {
								otherAgentConstraintsCopy.put(step, stepConstraintsEntry.getValue());
							}
						}
						constraints.put(otherAgent, otherAgentConstraintsCopy);
						otherAgentConstraints = otherAgentConstraintsCopy;
					}

					otherAgentConstraints.put(collisionStep, stepConstraintsCopy);
					assert constraints.get(otherAgent).get(collisionStep).size() == stepConstraints.size() - 1;
				}
			}
		}
	}

	protected static @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> copyConstraints(final @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, final Agent agent) {
		final @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> newConstraints = new HashMap<>(constraints.size());
		for (Map.@NotNull Entry<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> agentConstraints : constraints.entrySet()) {
			final Agent mapAgent = agentConstraints.getKey();
			Map<Long, Collection<Pair<Integer, Integer>>> oldConstraints = agentConstraints.getValue();
			final Map<Long, Collection<Pair<Integer, Integer>>> agentConstraintsMap = mapAgent.equals(agent) ? new HashMap<>(oldConstraints) : oldConstraints;
			newConstraints.put(mapAgent, agentConstraintsMap);
		}

		return newConstraints;
	}

	/**
	 * @param agents Set of agents to be planned
	 * @param step   Number of step in which agents should be planned
	 * @return
	 */
	@Override
	public Collection<Agent> planAgents(@NotNull Collection<Agent> agents, long step) {
		if (agents.isEmpty()) {
			return agents;
		}
		filterStepOccupiedVertices(step);
		simple = false;

		System.out.println("Starting computing " + step);

		Thread timer = new Thread(() -> {
			try {
				Thread.sleep(simpleStrategyAfter);
				simpleLock.lock();
				simple = true;
			} catch (InterruptedException ignored) {
			} finally {
				if (simpleLock.isHeldByCurrentThread()) {
					simpleLock.unlock();
				}
			}
		});
		timer.start();

		Collection<Agent> plannedAgents = planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), getExitIDs(a)))), step);
		timer.interrupt();

		System.out.println("Finished computing " + step);

		return plannedAgents;
	}

	/**
	 * @param agentsEntriesExits Set of agents to be planned TODO
	 * @param step               Number of step in which agents should be planned
	 * @return
	 */
	@Override
	public Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		initialPaths.clear();
		findInitialPaths(agentsEntriesExits, step);
		assert initialPaths.keySet().containsAll(agentsEntriesExits.keySet());

		final @NotNull Node node = bestValidNode(agentsEntriesExits, step);
		node.getAgents().forEach(this::addPlannedAgent);

		return node.getAgents();
	}

	protected void findInitialPaths(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		for (@NotNull Iterator<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator(); it.hasNext(); ) {
			final Map.Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExits = it.next();
			final Agent agent = agentEntryExits.getKey();
			final int entryID = agentEntryExits.getValue().getVal0();
			final Set<Integer> exitsIDs = agentEntryExits.getValue().getVal1();

			final @Nullable List<Integer> path = getPath(agent, step, entryID, exitsIDs, Collections.emptyMap());
			if (path == null) {
				it.remove();
			} else {
				agent.setPath(path, step);
				initialPaths.put(agent, path);
			}
		}
	}

	protected @NotNull Node bestValidNode(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		final @NotNull PriorityQueue<Node> queue = new PriorityQueue<>();
		queue.add(new Node(agentsEntriesExits.keySet()));

		long nodes = 0;

		@NotNull Set<Node> visitedStates = new HashSet<>();

		while (!(queue.isEmpty() || stopped)) {
			nodes++;

			final Node node = queue.poll();
			if (visitedStates.contains(node)) {
				continue;
			}
			visitedStates.add(node);

			final @NotNull Optional<Quaternion<Agent, Agent, Long, Boolean>> collidingAgentsOptional = collidingAgents(node.getAgents());
			if (collidingAgentsOptional.isEmpty()) {
				System.out.println("Step " + step + " nodes count: " + nodes);
				return node;
			}

			if (nodes % 4096 == 0) {
				System.out.println(nodes);
			}

			final @NotNull Quaternion<Agent, Agent, Long, Boolean> collidingAgents = collidingAgentsOptional.get();

			@NotNull Thread thread = new Thread(() -> replanAgent(agentsEntriesExits, step, queue, node, collidingAgents));
			thread.start();

			final @NotNull Quaternion<Agent, Agent, Long, Boolean> switchedCollisionAgents = switchCollisionAgents(collidingAgents);
			replanAgent(agentsEntriesExits, step, queue, node, switchedCollisionAgents);
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		if (!stopped) {
			throw new IllegalStateException("No valid node found in CBS");
		}

		return new Node(Collections.emptySet());
	}

	private void replanAgent(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, @NotNull Quaternion<Agent, Agent, Long, Boolean> collision) {
		final Agent agent = collision.getVal0();

		final boolean simpleStrategy;
		simpleLock.lock();
		simpleStrategy = simple;
		simpleLock.unlock();
		if (simpleStrategy) {
			applySimpleAlgorithm(step, queue, node, collision, agent);
			return;
		}

		final long collisionStep = collision.getVal2();
		final int entryID = agentsEntriesExits.get(agent).getVal0();
		final Set<Integer> exitsIDs = agentsEntriesExits.get(agent).getVal1();
		final @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints = copyConstraints(node.getConstraints(), agent);
		@NotNull Map<Long, Collection<Pair<Integer, Integer>>> agentConstraints = constraints.computeIfAbsent(agent, k -> new HashMap<>());
		Collection<Pair<Integer, Integer>> stepConstraints = agentConstraints.getOrDefault(collisionStep, Collections.emptySet());
		@NotNull Collection<Pair<Integer, Integer>> stepConstraintsCopy = new HashSet<>(stepConstraints);
		agentConstraints.put(collisionStep, stepConstraintsCopy);

		@NotNull Pair<Integer, Integer> collisionEntry = createCollision(agent, collision);
		stepConstraintsCopy.add(collisionEntry);

		final @Nullable LinkedList<Integer> path = getPath(agent, step, entryID, exitsIDs, agentConstraints);
		assignNewPath(step, queue, node, agent, collision, constraints, path);
	}

	protected void applySimpleAlgorithm(long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, @NotNull Quaternion<Agent, Agent, Long, Boolean> collision, Agent agent) {
		final Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints = copyConstraints(node.getConstraints(), agent);
		removeAgentFromConstraints(node, constraints, agent);
		final Collection<Agent> agents = replanAgents(step, node.getAgents(), agent);
		queue.add(new Node(agents, constraints, node, collision));
	}

	protected void assignNewPath(final long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, @NotNull Agent agent, Quaternion<Agent, Agent, Long, Boolean> collision, @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, @Nullable LinkedList<Integer> path) {
		final Collection<Agent> agents;

		if (path != null) {
			agents = copyAgents(step, node, agent, path);
		} else {
			removeAgentFromConstraints(node, constraints, agent);
			agents = replanAgents(step, node.getAgents(), agent);
		}

		queue.add(new Node(agents, constraints, node, collision));
	}

	protected @NotNull Collection<Agent> replanAgents(final long step, @NotNull Collection<Agent> agents, final Agent agent) {
		return agents.stream()
			.filter(a -> !a.equals(agent))
			.map(a -> {
				final @NotNull Agent agentCopy = new Agent(a);

				assert initialPaths.containsKey(a);
				agentCopy.setPath(initialPaths.get(a), getInitialPlannedStep(a, step));
				return agentCopy;
			})
			.collect(Collectors.toSet());
	}

	protected long getInitialPlannedStep(final Agent agent, final long step) {
		return step;
	}

	protected @NotNull Optional<Quaternion<Agent, Agent, Long, Boolean>> collidingAgents(final @NotNull Collection<Agent> agents) {
		Agent[] visitedAgents = new Agent[agents.size()];
		int i = 0;
		for (final @NotNull Iterator<Agent> it0 = agents.iterator(); it0.hasNext(); i++) {
			final Agent agent0 = it0.next();
			for (int j = 0; j < i; j++) {
				final Agent agent1 = visitedAgents[j];
				@NotNull Optional<Pair<Long, Boolean>> stepCollision = inCollision(agent0, agent1);
				if (stepCollision.isPresent()) {
					return Optional.of(new Quaternion<>(agent0, agent1, stepCollision.get().getVal0(), stepCollision.get().getVal1()));
				}
			}

			visitedAgents[i] = agent0;
		}

		return Optional.empty();
	}

	protected class Node implements Comparable<Node> {
		final @NotNull Collection<Agent> agents;
		final Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints;
		final @NotNull Distance distance;
		final @Nullable Node parent;
		final @Nullable Quaternion<Agent, Agent, Long, Boolean> collision;

		public Node(@NotNull Collection<Agent> agents) {
			this.agents = agents;
			this.distance = getDistance(agents);
			this.constraints = new HashMap<>();
			this.parent = null;
			this.collision = null;
		}

		@TestOnly
		public Node(@Nullable Node parent, Quaternion<Agent, Agent, Long, Boolean> collision) {
			this.agents = Collections.emptySet();
			this.distance = new Distance();
			this.constraints = new HashMap<>();
			this.parent = parent;
			this.collision = collision;
		}

		public Node(@NotNull Collection<Agent> agents, Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, Node parent, Quaternion<Agent, Agent, Long, Boolean> collision) {
			this.agents = agents;
			this.distance = getDistance(agents);
			this.constraints = constraints;
			this.parent = parent;
			this.collision = collision;
		}

		@NotNull
		private Distance getDistance(@NotNull Collection<Agent> agents) {
			return agents.stream().map(a -> new Distance(a.getPath(), graph)).reduce(Distance::new).orElse(new Distance());
		}

		public @NotNull Collection<Agent> getAgents() {
			return agents;
		}

		public Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> getConstraints() {
			return constraints;
		}

		public @Nullable Node getParent() {
			return parent;
		}

		public Quaternion<Agent, Agent, Long, Boolean> getCollision() {
			return collision;
		}

		/**
		 * @param o the object to be compared.
		 * @return
		 */
		@Override
		public int compareTo(@NotNull CBSSingleGrouped.Node o) {
			final int agentsComparison = Integer.compare(o.agents.size(), agents.size());
			if (agentsComparison == 0) {
				return distance.compareTo(o.distance);
			}
			return agentsComparison;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Node node)) {
				return false;
			}

			if (!agents.equals(node.agents)) {
				return false;
			}
			return distance.equals(node.distance) && constraints.equals(node.constraints);
		}

		@Override
		public int hashCode() {
			int result = agents.hashCode();
			result = 31 * result + constraints.hashCode();
			return result;
		}
	}
}
