package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph.VertexWithDirection;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.*;
import java.util.stream.Collectors;

public class AStar extends SafeLines {

	protected static final String MAXIMUM_VERTEX_VISITS_NAME = "Maximum vertex visits";
	protected static final int MAXIMUM_VERTEX_VISITS_DEF = 2;
	protected static final String ALLOW_AGENT_STOP_NAME = "Allow agent stop";
	protected static final boolean ALLOW_AGENT_STOP_DEF = false;

	public static final Map<String, Object> PARAMETERS = Map.of(SAFE_DISTANCE_NAME, SAFE_DISTANCE_DEF, MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF, ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);


	private final int maximumVertexVisits;
	private final boolean allowAgentStop;

	public AStar(SimulationGraph graph) {
		super(graph);

		maximumVertexVisits = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		allowAgentStop = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		double agentsPerimeter = agent.getAgentPerimeter();

		Map<Integer, Set<Long>> stepVisitedVertices = new HashMap<>(graph.getVertices().length * 2);

		if (stepOccupiedVertices.putIfAbsent(step, new HashMap<>()) != null && !safeVertex(step, agent.getEntry(), agentsPerimeter)) {
			return null;
		}

		Set<Integer> exitIDs;
		if (agent.getExit() > 0) {
			exitIDs = Set.of(agent.getExit());
		} else {
			exitIDs = directionExits.get(agent.getExitDirection()).stream().map(Vertex::getID).collect(Collectors.toSet());
		}

		Map<Integer, Double> heuristic = new HashMap<>();
		int entryID = agent.getEntry();
		double startEstimate = getHeuristic(exitIDs, entryID);
		heuristic.put(entryID, startEstimate);

		PriorityQueue<State> queue = new PriorityQueue<>();
		queue.add(new State(graph.getVertex(entryID), startEstimate, step));

		State state;
		while ((state = queue.poll()) != null) {
			int vertexID = state.getID();
			stepVisitedVertices.putIfAbsent(vertexID, new HashSet<>());

			if (heuristic.get(vertexID) < 0 || stepVisitedVertices.get(vertexID).contains(state.step)) {
				continue;
			}
			stepVisitedVertices.get(vertexID).add(state.getStep());
//			else {  TODO
//				heuristic.put(vertexID, -1.);
//			}

			if (exitIDs.contains(vertexID)) {
				LinkedList<Integer> path = new LinkedList<>();
				while (state != null) {
					int id = state.getID();
					path.addFirst(id);
					stepOccupiedVertices.get(state.getStep()).put(id, agent);

					state = (State) state.getParent();
				}

				agent.setPath(path, step);
				return agent;
			}

			long stateStep = state.getStep();
			for (int neighbourID : state.getVertex().getNeighbourIDs()) {
				long neighbourStep = stateStep + 1;
				heuristic.putIfAbsent(neighbourID, getHeuristic(exitIDs, neighbourID));
				double estimate;

				if (Double.isFinite(estimate = heuristic.get(neighbourID)) &&
					canVisitVertex(state, neighbourID) &&
					(
						stepOccupiedVertices.putIfAbsent(neighbourStep, new HashMap<>()) == null
							|| (
							safeVertex(neighbourStep, neighbourID, agentsPerimeter) &&
								safeStepTo(neighbourStep, neighbourID, state.getID(), agentsPerimeter)
						)
					) &&
					safeStepFrom(stateStep, state.getID(), neighbourID, agentsPerimeter)
				) {
					double distance = graph.getDistance(vertexID, neighbourID);
					estimate -= 0.015625 * (distance + state.getDistance()) + 0.00390625 * (neighbourStep - step);
					queue.add(new State(state, graph.getVertex(neighbourID), distance, estimate));
				}
			}

			if (
				allowAgentStop && vertexID != entryID &&
					canVisitVertex(state, vertexID)
					&& (
					stepOccupiedVertices.putIfAbsent(stateStep + 1, new HashMap<>()) == null || safeVertex(stateStep + 1, vertexID, agentsPerimeter)
				)
			) {
				queue.add(new State(state));
			}
		}

		return null;
	}

	private double getHeuristic(Set<Integer> exitIDs, int entryID) {
		return exitIDs.stream().mapToDouble(exitID -> graph.getDistance(entryID, exitID)).min().orElse(0);
	}

	private boolean canVisitVertex(VertexWithDirection state, int vertexID) {
		int visitCount = 0;
		while (state != null) {
			if (state.getID() == vertexID) {
				visitCount++;
				if (visitCount >= maximumVertexVisits) {
					return false;
				}
			}
			state = state.getParent();
		}

		return true;
	}

	private static class State extends VertexWithDirection {
		private final long step;

		public State(GraphicalVertex vertex, double estimate, long step) {
			super(vertex, 0, estimate);
			this.step = step;
		}

		public State(State state) {
			super(state, state.getVertex(), 1, state.getEstimate() - 0.00390625);
			step = state.getStep() + 1;
		}

		public State(State previous, GraphicalVertex actual, double distance, double estimate) {
			super(previous, actual, distance, estimate);
			this.step = previous.getStep() + 1;
		}

		public long getStep() {
			return step;
		}
	}
}
