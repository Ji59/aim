package cz.cuni.mff.kotal.backend.algorithm.cbs;

import cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Quaternion;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirection.Distance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CBSSingleGrouped extends AStarSingle {
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(AStarSingle.PARAMETERS);

	public CBSSingleGrouped(SimulationGraph graph) {
		super(graph);
	}

	@TestOnly
	protected CBSSingleGrouped(SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn) {
		super(graph, safeDistance, maximumVertexVisits, allowAgentStop, maximumPathDelay, allowAgentReturn);
	}

	/**
	 * @param agents Set of agents to be planned
	 * @param step   Number of step in which agents should be planned
	 * @return
	 */
	@Override
	public Collection<Agent> planAgents(@NotNull Collection<Agent> agents, long step) {
		filterStepOccupiedVertices(step);
		return planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), getExitIDs(a)))), step);
	}

	/**
	 * @param agentsEntriesExits Set of agents to be planned TODO
	 * @param step               Number of step in which agents should be planned
	 * @return
	 */
	@Override
	public Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		findInitialPaths(agentsEntriesExits, step);

		final Node node = bestValidNode(agentsEntriesExits, step);
		node.getAgents().forEach(this::addPlannedAgent);

		return node.getAgents();
	}

	protected void findInitialPaths(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		for (Iterator<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator(); it.hasNext(); ) {
			final Map.Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExits = it.next();
			final Agent agent = agentEntryExits.getKey();
			final int entryID = agentEntryExits.getValue().getVal0();
			final Set<Integer> exitsIDs = agentEntryExits.getValue().getVal1();

			final List<Integer> path = getPath(agent, step, entryID, exitsIDs, Collections.emptyMap());
			if (path == null) {
				it.remove();
			} else {
				agent.setPath(path, step);
			}
		}
	}

	protected @NotNull Node bestValidNode(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		final PriorityQueue<Node> queue = new PriorityQueue<>();
		queue.add(new Node(agentsEntriesExits.keySet()));

		long nodes = 0;

		Set<Node> visitedStates = new HashSet<>();

		while (!(queue.isEmpty() || stopped)) {
			nodes++;

			final Node node = queue.poll();
			if (visitedStates.contains(node)) {
				continue;
			}
			visitedStates.add(node);

			final Optional<Quaternion<Agent, Agent, Long, Boolean>> collidingAgentsOptional = collidingAgents(node.getAgents());
			if (collidingAgentsOptional.isEmpty()) {
				System.out.println("Step " + step + " nodes count: " + nodes);
				return node;
			}

			if (nodes % 4096 == 0) {
				System.out.println(nodes);
			}

			final Quaternion<Agent, Agent, Long, Boolean> collidingAgents = collidingAgentsOptional.get();

			Thread thread = new Thread(() -> replanAgent(agentsEntriesExits, step, queue, node, collidingAgents));
			thread.start();

			final Quaternion<Agent, Agent, Long, Boolean> switchedCollisionAgents = switchCollisionAgents(collidingAgents);
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

	@NotNull
	private static Quaternion<Agent, Agent, Long, Boolean> switchCollisionAgents(Quaternion<Agent, Agent, Long, Boolean> collidingAgents) {
		return new Quaternion<>(collidingAgents.getVal1(), collidingAgents.getVal0(), collidingAgents.getVal2(), collidingAgents.getVal3());
	}

	private void replanAgent(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, Quaternion<Agent, Agent, Long, Boolean> collision) {
		final Agent agent = collision.getVal0();

		final long collisionStep = collision.getVal2();
		final int entryID = agentsEntriesExits.get(agent).getVal0();
		final Set<Integer> exitsIDs = agentsEntriesExits.get(agent).getVal1();
		final Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints = copyConstraints(node.getConstraints(), agent);
		Map<Long, Collection<Pair<Integer, Integer>>> agentConstraints = constraints.computeIfAbsent(agent, k -> new HashMap<>());
		Collection<Pair<Integer, Integer>> stepConstraints = agentConstraints.getOrDefault(collisionStep, Collections.emptySet());
		Collection<Pair<Integer, Integer>> stepConstraintsCopy = new HashSet<>(stepConstraints);
		agentConstraints.put(collisionStep, stepConstraintsCopy);

		Pair<Integer, Integer> collisionEntry = createCollision(agent, collision);
		stepConstraintsCopy.add(collisionEntry);

		final LinkedList<Integer> path = getPath(agent, step, entryID, exitsIDs, agentConstraints);
		assignNewPath(agentsEntriesExits, step, queue, node, agent, collision, constraints, path);
	}

	@NotNull
	public static Pair<Integer, Integer> createCollision(@NotNull Agent agent, Quaternion<Agent, Agent, Long, Boolean> collision) {
		final long collisionStep = collision.getVal2();
		final boolean collisionOnTravel = collision.getVal3();

		final int collisionRelativeTime = (int) (collisionStep - agent.getPlannedTime());
		final List<Integer> agentPath = agent.getPath();
		final int collisionStepVertex = agentPath.get(collisionRelativeTime);

		return new Pair<>(collisionOnTravel ? collisionStepVertex : agentPath.get(collisionRelativeTime - 1), collisionStepVertex);
	}

	protected void assignNewPath(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, @NotNull Agent agent, Quaternion<Agent, Agent, Long, Boolean> collision, Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, LinkedList<Integer> path) {
		Collection<Agent> agents;
		if (path != null) {
			agents = copyAgents(step, node, agent, path);
		} else {
			removeAgentFromConstraints(node, constraints, agent);
			agents = replanAgents(agentsEntriesExits, step, constraints, node.getAgents(), agent);
		}

		queue.add(new Node(agents, constraints, node, collision));
	}

	@NotNull
	protected static Collection<Agent> copyAgents(long step, @NotNull Node node, @NotNull Agent agent, LinkedList<Integer> path) {
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

	protected static void removeAgentFromConstraints(final Node parentNode, final @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, final Agent agent) {
		constraints.remove(agent);

		final Set<Agent> changedAgents = new HashSet<>(parentNode.getAgents().size());

		for (Node parent = parentNode; parent.getCollision() != null; parent = parent.getParent()) {
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
					final Set<Pair<Integer, Integer>> stepConstraintsCopy = new HashSet<>(stepConstraints.size());
					for (final Pair<Integer, Integer> constraint : stepConstraints) {
						if (!constraint.equals(collisionConstraint)) {
							stepConstraintsCopy.add(constraint);
						}
					}

					if (!changedAgents.contains(otherAgent)) {
						changedAgents.add(otherAgent);
						final Map<Long, Collection<Pair<Integer, Integer>>> otherAgentConstraintsCopy = new HashMap<>(otherAgentConstraints.size());
						for (Map.Entry<Long, Collection<Pair<Integer, Integer>>> stepConstraintsEntry : otherAgentConstraints.entrySet()) {
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

	protected Collection<Agent> replanAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, @NotNull Collection<Agent> agents, final Agent agent) {
		return agents.stream().parallel()
			.filter(a -> !a.equals(agent))
			.map(a -> {
				final Agent agentCopy = new Agent(a);
				final Pair<Integer, Set<Integer>> entryExits = agentsEntriesExits.get(a);
				final int entry = entryExits.getVal0();
				final Set<Integer> exits = entryExits.getVal1();
				final List<Integer> newPath = getPath(agentCopy, step, entry, exits, constraints.getOrDefault(a, Collections.emptyMap()));
				assert newPath != null || stopped;
				agentCopy.setPath(newPath, step);
				return agentCopy;
			})
			.collect(Collectors.toSet());
	}

	protected static @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> copyConstraints(final @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, final Agent agent) {
		final Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> newConstraints = new HashMap<>(constraints.size());
		for (Map.Entry<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> agentConstraints : constraints.entrySet()) {
			final Agent mapAgent = agentConstraints.getKey();
			Map<Long, Collection<Pair<Integer, Integer>>> oldConstraints = agentConstraints.getValue();
			final Map<Long, Collection<Pair<Integer, Integer>>> agentConstraintsMap = mapAgent.equals(agent) ? new HashMap<>(oldConstraints) : oldConstraints;
			newConstraints.put(mapAgent, agentConstraintsMap);
		}

		return newConstraints;
	}

	protected Optional<Quaternion<Agent, Agent, Long, Boolean>> collidingAgents(final @NotNull Collection<Agent> agents) {
		int i = 0;
		for (final Iterator<Agent> it0 = agents.iterator(); it0.hasNext(); i++) {
			final Agent agent0 = it0.next();
			int j = 0;
			for (final Iterator<Agent> it1 = agents.iterator(); it1.hasNext(); j++) {
				Agent agent1 = it1.next();
				if (j <= i) {
					continue;
				}

				Optional<Pair<Long, Boolean>> stepCollision = inCollision(agent0, agent1);
				if (stepCollision.isPresent()) {
					return Optional.of(new Quaternion<>(agent0, agent1, stepCollision.get().getVal0(), stepCollision.get().getVal1()));
				}
			}
		}

		return Optional.empty();
	}

	protected class Node implements Comparable<Node> {
		final @NotNull Collection<Agent> agents;
		final Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints;
		final @NotNull Distance distance;
		final @Nullable Node parent;
		final Quaternion<Agent, Agent, Long, Boolean> collision;

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

		public Collection<Agent> getAgents() {
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
			final int agentsComparison = Integer.compare(agents.size(), o.agents.size());
			if (agentsComparison == 0) {
				return distance.compareTo(o.distance);
			}
			return agentsComparison;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Node node)) return false;

			if (!agents.equals(node.agents)) return false;
			return constraints.equals(node.constraints);
		}

		@Override
		public int hashCode() {
			int result = agents.hashCode();
			result = 31 * result + constraints.hashCode();
			return result;
		}
	}
}
