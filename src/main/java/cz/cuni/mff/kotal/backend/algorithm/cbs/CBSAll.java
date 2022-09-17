package cz.cuni.mff.kotal.backend.algorithm.cbs;

import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Quaternion;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class CBSAll extends CBSSingleGrouped {
	public static final Map<String, Object> PARAMETERS = CBSSingleGrouped.PARAMETERS;

	protected final Map<Agent, Pair<Agent, Long>> notFinishedAgents = new HashMap<>();

	public CBSAll(SimulationGraph graph) {
		super(graph);
	}

	@TestOnly
	protected CBSAll(SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn) {
		super(graph, safeDistance, maximumVertexVisits, allowAgentStop, maximumPathDelay, allowAgentReturn);
	}

	@Override
	public Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		findInitialPaths(agentsEntriesExits, step);

		Map<Agent, Pair<Integer, Set<Integer>>> allAgents = new LinkedHashMap<>(agentsEntriesExits.size() + notFinishedAgents.size());
		allAgents.putAll(agentsEntriesExits);

		final Iterator<Agent> it = notFinishedAgents.keySet().iterator();
		while (it.hasNext()) {
			final Agent agent = it.next();

			final long plannedTime = agent.getPlannedTime();
			final int travelTime = (int) (step - plannedTime);
			if (travelTime >= agent.getPath().size() - 1) {
				it.remove();
			} else {
				final int startingVertexID = agent.getPath().get(travelTime);
				allAgents.put(agent, new Pair<>(startingVertexID, getExits(agent)));
			}
		}

		final Node node = bestValidNode(allAgents, step);

		final Collection<Agent> plannedAgents = node.getAgents();
		processPlannedAgents(plannedAgents, step);

		return plannedAgents;
	}

	protected void processPlannedAgents(Collection<Agent> plannedAgents, long step) {
		if (!plannedAgents.containsAll(notFinishedAgents.keySet())) {
			throw new AssertionError();
		}

		final Iterator<Agent> iterator = plannedAgents.iterator();
		while (iterator.hasNext()) {
			final Agent agent = iterator.next();
			if (notFinishedAgents.containsKey(agent)) {
				iterator.remove();

				List<Integer> pathEnd = agent.getPath();

				final Pair<Agent, Long> plannedPathTriplet = notFinishedAgents.get(agent);
				final Agent originalAgent = plannedPathTriplet.getVal0();
				final long plannedTime = plannedPathTriplet.getVal1();

				if (agent.getPlannedTime() == plannedTime) {
					continue;
				}

				final List<Integer> lastPath = originalAgent.getPath();
				for (int i = 0; i < step - plannedTime; i++) {
					pathEnd.add(i, lastPath.get(i));
				}
				originalAgent.setPath(pathEnd);
			} else {
				notFinishedAgents.put(agent, new Pair<>(agent, agent.getPlannedTime()));
			}
		}
	}

	@Override
	protected void assignNewPath(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, @NotNull Agent agent, Quaternion<Agent, Agent, Long, Boolean> collision, Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, LinkedList<Integer> path) {
		Collection<Agent> agents;

		if (path != null) {
			agents = copyAgents(step, node, agent, path);
		} else if (notFinishedAgents.containsKey(agent)) {
			return;
		} else {
			removeAgentFromConstraints(node, constraints, agent);
			agents = replanAgents(agentsEntriesExits, step, constraints, node.getAgents(), agent);
		}

		queue.add(new Node(agents, constraints, node, collision));
	}

	@Override
	public void addPlannedAgent(Agent agent) {
		setLargestAgentPerimeter(agent);
	}

	@Override
	public void addPlannedPath(Agent agent, List<Integer> path, long step) {
		// stepOccupiedVertices should be empty
	}
}
