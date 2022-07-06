package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.backend.algorithm.AStarRoundabout.LinkGraph;
import cz.cuni.mff.kotal.backend.algorithm.AStarRoundabout.LinkVertex;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SATRoundabout extends SATPlanner {
	protected static final String LANES_NAME = "Lanes";
	protected static final Integer LANES_DEF = 2;
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SATPlanner.PARAMETERS);

	static {
		PARAMETERS.put(LANES_NAME, LANES_DEF);
	}

	private final int lanes;
	private final Map<Integer, Set<Integer>> directionExits = new HashMap<>();

	public SATRoundabout(SimulationGraph graph) {
		this(graph, true);
	}

	public SATRoundabout(SimulationGraph graph, boolean oriented) {
		super(AStarRoundabout.createRoundaboutGraph(graph, AlgorithmMenuTab2.getIntegerParameter(LANES_NAME, LANES_DEF), oriented));
		lanes = AlgorithmMenuTab2.getIntegerParameter(LANES_NAME, LANES_DEF);
		graph.getEntryExitVertices().forEach((key, value) ->
			directionExits.put(
				key,
				value.stream()
					.filter(vertex -> vertex.getType().isExit())
					.map(v -> ((LinkGraph) this.graph).getLinkID(v.getID()))
					.collect(Collectors.toSet())
			)
		);
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		LinkGraph linkGraph = (LinkGraph) graph;
		Map<Agent, Pair<Integer, Set<Integer>>> agentsLinkEntriesExits = agents.stream()
			.collect(Collectors.toMap(
					Function.identity(),
					a -> new Pair<>(
						linkGraph.getLinkID(a.getEntry()),
						a.getExit() < 0 ?
							directionExits.get(a.getExitDirection()) :
							Collections.singleton(linkGraph.getLinkID(a.getExit()))
					)
				)
			);
		return transferPaths(agentsLinkEntriesExits, step);
	}

	@NotNull
	private Collection<Agent> transferPaths(Map<Agent, Pair<Integer, Set<Integer>>> agentsLinkEntriesExits, long step) {
		Collection<Agent> plannedAgents = super.planAgents(agentsLinkEntriesExits, step);
		plannedAgents.forEach(agent -> transferPath(agent, step));
		return plannedAgents;
	}

	private void transferPath(Agent agent, long step) {
		List<Integer> path = agent.getPath().stream().map(id -> ((LinkVertex) graph.getVertex(id)).getRealID()).toList();
		agent.setPath(path, step);
	}

	@Override
	public Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		LinkGraph linkGraph = (LinkGraph) graph;
		Map<Agent, Pair<Integer, Set<Integer>>> agentsLinkEntriesExits = agentsEntriesExits.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				e -> new Pair<>(
					linkGraph.getLinkID(e.getValue().getVal0()),
					e.getValue().getVal1().stream()
						.map(linkGraph::getLinkID)
						.collect(Collectors.toSet())
				)
			));
		return transferPaths(agentsLinkEntriesExits, step);
	}

	@Override
	public Agent planAgent(Agent agent, int entryID, Set<Integer> exitsIDs, long step) {
		LinkGraph linkGraph = (LinkGraph) graph;
		Agent plannedAgent = super.planAgent(agent, linkGraph.getLinkID(entryID), exitsIDs.stream().map(linkGraph::getLinkID).collect(Collectors.toSet()), step);
		if (plannedAgent != null) {
			transferPath(plannedAgent, step);
		}
		return plannedAgent;
	}
}
