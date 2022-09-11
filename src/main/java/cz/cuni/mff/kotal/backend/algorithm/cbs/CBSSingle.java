package cz.cuni.mff.kotal.backend.algorithm.cbs;

import cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CBSSingle extends AStarSingle {
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(AStarSingle.PARAMETERS);

	public CBSSingle(SimulationGraph graph) {
		super(graph);
	}

	@TestOnly
	protected CBSSingle(SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn) {
		super(graph, safeDistance, maximumVertexVisits, allowAgentStop, maximumPathDelay, allowAgentReturn);
	}

	/**
	 * @param agents Set of agents to be planned
	 * @param step   Number of step in which agents should be planned
	 * @return
	 */
	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		filterStepOccupiedVertices(step);
		return planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), getExitIDs(a)))), step);
	}

	/**
	 * @param agentsEntriesExits Set of agents to be planned TODO
	 * @param step               Number of step in which agents should be planned
	 * @return
	 */
	@Override
	public Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		for (Iterator<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator(); it.hasNext(); ) {
			final Map.Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExits = it.next();
			final int entry = agentEntryExits.getValue().getVal0();
			final Set<Integer> exits = agentEntryExits.getValue().getVal1();
			final Agent agent = planAgent(agentEntryExits.getKey(), entry, exits, step);
			if (agent == null) {
				it.remove();
			}
		}

		Optional<Triplet<Agent, Agent, Long>> collidingAgentsOptional;
		while ((collidingAgentsOptional = collidingAgents(agentsEntriesExits.keySet())).isPresent()) {
			final Triplet<Agent, Agent, Long> collidingAgents = collidingAgentsOptional.get();


		}

		return agentsEntriesExits.keySet();
	}

	protected Optional<Triplet<Agent, Agent, Long>> collidingAgents(final Collection<Agent> agents) {
		int i = 0;
		for (final Iterator<Agent> it0 = agents.iterator(); it0.hasNext(); i++) {
			final Agent agent0 = it0.next();
			int j = 0;
			for (final Iterator<Agent> it1 = agents.iterator(); it1.hasNext(); j++) {
				Agent agent1 = it1.next();
				if (j < i) {
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

	protected static class Node implements Comparable<Node> {
		final Map<Agent, Pair<Integer, Set<Integer>>> agents;
		final double cost;

		protected Node(Map<Agent, Pair<Integer, Set<Integer>>> agents, double cost) {
			this.agents = agents;
			this.cost = cost;
		}

		/**
		 * @param o the object to be compared.
		 * @return
		 */
		@Override
		public int compareTo(@NotNull CBSSingle.Node o) {
			final int agentsComparison = Integer.compare(agents.size(), o.agents.size());
			if (agentsComparison == 0) {
				return Double.compare(cost, o.cost);
			}
			return agentsComparison;
		}
	}
}
