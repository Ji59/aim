package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.backend.algorithm.LinkGraph;
import cz.cuni.mff.kotal.backend.algorithm.LinkVertex;
import cz.cuni.mff.kotal.backend.algorithm.astar.AStarRoundabout;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SATRoundabout extends SATSingleGrouped {
	/**
	 * Parameters names and default values
	 */
	protected static final String LANES_NAME = "Lanes";
	protected static final Integer LANES_DEF = 2;
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SATSingleGrouped.PARAMETERS);

	static {
		PARAMETERS.put(LANES_NAME, LANES_DEF);
	}

	private final int lanes;
	private final Map<Integer, Set<Integer>> directionExits = new HashMap<>();

	public SATRoundabout(@NotNull SimulationGraph graph) {
		this(graph, true);
	}

	public SATRoundabout(@NotNull SimulationGraph graph, boolean oriented) {
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
	public Collection<Agent> planAgents(@NotNull Collection<Agent> agents, long step) {
		LinkGraph linkGraph = (LinkGraph) graph;
		@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsLinkEntriesExits = agents.stream()
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
		return super.planAgents(agentsLinkEntriesExits, step);
	}

	private void transferPath(@NotNull Agent agent) {
		@NotNull List<Integer> path = agent.getPath().stream().map(id -> ((LinkVertex) graph.getVertex(id)).getRealID()).toList();
		agent.setPath(path, agent.getPlannedStep());
	}

	@Override
	public Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		LinkGraph linkGraph = (LinkGraph) graph;
		@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsLinkEntriesExits = agentsEntriesExits.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				e -> new Pair<>(
					linkGraph.getLinkID(e.getValue().getVal0()),
					e.getValue().getVal1().stream()
						.map(linkGraph::getLinkID)
						.collect(Collectors.toSet())
				)
			));
		return super.planAgents(agentsLinkEntriesExits, step);
	}

	@Override
	public Agent planAgent(@NotNull Agent agent, int entryID, @NotNull Set<Integer> exitsIDs, long step) {
		LinkGraph linkGraph = (LinkGraph) graph;
		return super.planAgent(agent, linkGraph.getLinkID(entryID), exitsIDs.stream().map(linkGraph::getLinkID).collect(Collectors.toSet()), step);
	}

	protected void combinePaths(@NotNull Agent agent, @NotNull List<Integer> path, long step) {
		path.remove(path.size() - 1);
		LinkGraph linkGraph = (LinkGraph) graph;
		for (int id : agent.getPath()) {
			path.add(linkGraph.getLinkID(id));
		}
		agent.setPath(path, step);
	}
}
