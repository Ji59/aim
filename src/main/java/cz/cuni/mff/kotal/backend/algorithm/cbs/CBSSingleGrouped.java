package cz.cuni.mff.kotal.backend.algorithm.cbs;

import cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
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

		final Node node = bestValidNode(agentsEntriesExits, step);
		node.getAgents().forEach(this::addPlannedAgent);

		return node.getAgents();
	}

	protected @NotNull Node bestValidNode(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		final PriorityQueue<Node> queue = new PriorityQueue<>();
		queue.add(new Node(agentsEntriesExits.keySet()));

		long nodes = 0;

		while (!queue.isEmpty()) {
			nodes++;

			final Node node = queue.poll();
			final Optional<Triplet<Agent, Agent, Long>> collidingAgentsOptional = collidingAgents(node.getAgents());
			if (collidingAgentsOptional.isEmpty()) {
				System.out.println("Step " + step + " nodes count: " + nodes);
				return node;
			}

			if (nodes % 4096 == 0) {
				System.out.println(nodes);
			}

			final Triplet<Agent, Agent, Long> collidingAgents = collidingAgentsOptional.get();
			final long collisionStep = collidingAgents.getVal2();

			final Agent agent0 = collidingAgents.getVal0();
			Thread thread = new Thread(() -> replanAgent(agentsEntriesExits, step, queue, node, collisionStep, agent0, collidingAgents));
			thread.start();

			final Agent agent1 = collidingAgents.getVal1();
			replanAgent(agentsEntriesExits, step, queue, node, collisionStep, agent1, collidingAgents);
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		throw new IllegalStateException("No valid node found in CBS");
	}

	private void replanAgent(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, long collisionStep, @NotNull Agent agent, Triplet<Agent, Agent, Long> collision) {
		final int entryID = agentsEntriesExits.get(agent).getVal0();
		final Set<Integer> exitsIDs = agentsEntriesExits.get(agent).getVal1();
		final Map<Agent, Map<Long, Collection<Integer>>> constraints = copyConstraints(node.getConstraints(), agent);
		Map<Long, Collection<Integer>> agentConstraints = constraints.computeIfAbsent(agent, k -> new HashMap<>());
		Collection<Integer> stepConstraints = agentConstraints.getOrDefault(collisionStep, Collections.emptySet());
		Collection<Integer> stepConstraintsCopy = new HashSet<>(stepConstraints);
		agentConstraints.put(collisionStep, stepConstraintsCopy);
		stepConstraintsCopy.add(agent.getPath().get((int) (collisionStep - step)));

		final Collection<Agent> agents = new HashSet<>(node.getAgents());
		agents.remove(agent);

		final LinkedList<Integer> path = getPath(agent, step, entryID, exitsIDs, agentConstraints);
		if (path != null) {
			final Agent agentCopy = new Agent(agent).setPath(path, step);
			agents.add(agentCopy);
		} else {
			Collection<Agent> nonOptimalAgents = removeAgentFromConstraints(node, constraints, agent, step);
			replanAgents(agentsEntriesExits, step, constraints, agents, nonOptimalAgents);
		}

		queue.add(new Node(agents, constraints, node, collision));
	}

	private void replanAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, @NotNull Map<Agent, Map<Long, Collection<Integer>>> constraints, @NotNull Collection<Agent> agents, @NotNull Collection<Agent> nonOptimalAgents) {
		nonOptimalAgents.stream().parallel().forEach(agent -> {
			agents.remove(agent);
			final Agent agentCopy = new Agent(agent);
			final Pair<Integer, Set<Integer>> entryExits = agentsEntriesExits.get(agent);
			final int entry = entryExits.getVal0();
			final Set<Integer> exits = entryExits.getVal1();
			final LinkedList<Integer> newPath = getPath(agentCopy, step, entry, exits, constraints.get(agent));
			assert newPath != null;
			agent.setPath(newPath);
			agents.add(agent);
		});
	}

	private static @NotNull Collection<Agent> removeAgentFromConstraints(final Node parentNode, final @NotNull Map<Agent, Map<Long, Collection<Integer>>> constraints, final Agent agent, final long step) {
		constraints.remove(agent);

		Collection<Agent> affectedAgents = new HashSet<>();

		for (Node parent = parentNode; parent.getCollision() != null; parent = parent.getParent()) {
			final Triplet<Agent, Agent, Long> parentCollision = parent.getCollision();
			Agent otherAgent = null;
			if (parentCollision.getVal0().equals(agent)) {
				otherAgent = parentCollision.getVal1();
			} else if (parentCollision.getVal1().equals(agent)) {
				otherAgent = parentCollision.getVal0();
			}

			if (otherAgent != null && constraints.containsKey(otherAgent)) {
				final Map<Long, Collection<Integer>> otherAgentConstraints = constraints.get(otherAgent);
				final long collisionStep = parentCollision.getVal2();
				if (!otherAgentConstraints.containsKey(collisionStep)) {
					continue;
				}
				final int collisionVertex = otherAgent.getPath().get((int) (collisionStep - step));
				final boolean removedConstraint = otherAgentConstraints.get(collisionStep).remove(collisionVertex);
				if (removedConstraint) {
					affectedAgents.add(otherAgent);
				}
			}
		}

		return affectedAgents;
	}

	protected static @NotNull Map<Agent, Map<Long, Collection<Integer>>> copyConstraints(final @NotNull Map<Agent, Map<Long, Collection<Integer>>> constraints, final Agent agent) {
		final Map<Agent, Map<Long, Collection<Integer>>> newConstraints = new HashMap<>(constraints.size());
		for (Map.Entry<Agent, Map<Long, Collection<Integer>>> agentConstraints : constraints.entrySet()) {
			final Agent mapAgent = agentConstraints.getKey();
			Map<Long, Collection<Integer>> oldConstraints = agentConstraints.getValue();
			final Map<Long, Collection<Integer>> agentConstraintsMap = mapAgent.equals(agent) ? new HashMap<>(oldConstraints) : oldConstraints;
			newConstraints.put(mapAgent, agentConstraintsMap);
		}

		return newConstraints;
	}

	protected @NotNull Optional<Triplet<Agent, Agent, Long>> collidingAgents(final @NotNull Collection<Agent> agents) {
		int i = 0;
		for (final Iterator<Agent> it0 = agents.iterator(); it0.hasNext(); i++) {
			final Agent agent0 = it0.next();
			int j = 0;
			for (final Iterator<Agent> it1 = agents.iterator(); it1.hasNext(); j++) {
				Agent agent1 = it1.next();
				if (j <= i) {
					continue;
				}

				Optional<Long> stepCollision = inCollision(agent0, agent1);
				if (stepCollision.isPresent()) {
					return Optional.of(new Triplet<>(agent0, agent1, stepCollision.get()));
				}
			}
		}

		return Optional.empty();
	}

	protected class Node implements Comparable<Node> {
		final @NotNull Collection<Agent> agents;
		final Map<Agent, Map<Long, Collection<Integer>>> constraints;
		final @NotNull Distance distance;
		final @Nullable Node parent;
		final @Nullable Triplet<Agent, Agent, Long> collision;

		public Node(@NotNull Collection<Agent> agents) {
			this.agents = agents;
			this.distance = getDistance(agents);
			this.constraints = new HashMap<>();
			this.parent = null;
			this.collision = null;
		}

		public Node(@NotNull Collection<Agent> agents, Map<Agent, Map<Long, Collection<Integer>>> constraints, Node parent, Triplet<Agent, Agent, Long> collision) {
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

		public Map<Agent, Map<Long, Collection<Integer>>> getConstraints() {
			return constraints;
		}

		public @Nullable Node getParent() {
			return parent;
		}

		public @Nullable Triplet<Agent, Agent, Long> getCollision() {
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
	}
}
