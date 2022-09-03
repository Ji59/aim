package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLines;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirectionParent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class AStarSingle extends SafeLines {
	protected static final String MAXIMUM_VERTEX_VISITS_NAME = "Maximum vertex visits";
	protected static final int MAXIMUM_VERTEX_VISITS_DEF = 2;
	protected static final String ALLOW_AGENT_STOP_NAME = "Allow agent stop";
	protected static final boolean ALLOW_AGENT_STOP_DEF = false;
	protected static final String MAXIMUM_PATH_DELAY_NAME = "Maximum path delay";
	protected static final int MAXIMUM_PATH_DELAY_DEF = Integer.MAX_VALUE;
	protected static final String ALLOW_AGENT_RETURN_NAME = "Allow agent to return";
	protected static final boolean ALLOW_AGENT_RETURN_DEF = false;

	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SafeLines.PARAMETERS);

	static {
		PARAMETERS.put(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		PARAMETERS.put(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
		PARAMETERS.put(MAXIMUM_PATH_DELAY_NAME, MAXIMUM_PATH_DELAY_DEF);
		PARAMETERS.put(ALLOW_AGENT_RETURN_NAME, ALLOW_AGENT_RETURN_DEF);
	}


	protected final int maximumVertexVisits;
	protected final boolean allowAgentStop;
	protected final int maximumPathDelay;
	protected final boolean allowAgentReturn;

	public AStarSingle(SimulationGraph graph) {
		super(graph);

		maximumVertexVisits = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		allowAgentStop = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
		maximumPathDelay = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_PATH_DELAY_NAME, MAXIMUM_PATH_DELAY_DEF);
		allowAgentReturn = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_RETURN_NAME, ALLOW_AGENT_RETURN_DEF);
	}

	@TestOnly
	protected AStarSingle(SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn) {
		super(graph, safeDistance);
		this.maximumVertexVisits = maximumVertexVisits;
		this.allowAgentStop = allowAgentStop;
		this.maximumPathDelay = maximumPathDelay;
		this.allowAgentReturn = allowAgentReturn;
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		return planAgent(agent, agent.getEntry(), Collections.singleton(agent.getExit()), step);
	}

	@Override
	public Agent planAgent(Agent agent, int entryID, Set<Integer> exitsIDs, long step) {
		LinkedList<Integer> path = getPath(agent, step, entryID, exitsIDs);
		if (path == null) {
			return null;
		}

		addPlannedAgent(agent.setPath(path, step));
		return agent;
	}

	@Nullable
	protected LinkedList<Integer> getPath(Agent agent, long step, int entryID, Set<Integer> exitsIDs) {
		if (stepOccupiedVertices.putIfAbsent(step, new HashMap<>()) != null && !safeVertex(step, entryID, agent.getAgentPerimeter())) {
			return null;
		}

		int maximumDelay = exitsIDs.stream().mapToInt(exitID -> (int) (Math.ceil(graph.getDistance(entryID, exitID)) + maximumPathDelay)).min().getAsInt();

		Map<Integer, Double> heuristic = new HashMap<>();
		double startEstimate = getHeuristic(exitsIDs, entryID);
		heuristic.put(entryID, startEstimate);

		PriorityQueue<State> queue = new PriorityQueue<>();
		queue.add(new State(graph.getVertex(entryID), startEstimate, step));

		return searchPath(agent, step, exitsIDs, heuristic, entryID, queue, maximumDelay);
	}

	protected LinkedList<Integer> searchPath(Agent agent, long step, Set<Integer> exitIDs, Map<Integer, Double> heuristic, int entryID, PriorityQueue<State> queue, int maximumDelay) {
		double agentsPerimeter = agent.getAgentPerimeter();

		Map<Integer, Set<Long>> stepVisitedVertices = new HashMap<>(graph.getVertices().length * 2);

		State state;
		while ((state = queue.poll()) != null) {
			int vertexID = state.getID();
			stepVisitedVertices.putIfAbsent(vertexID, new HashSet<>());

			if (heuristic.get(vertexID) < 0 || stepVisitedVertices.get(vertexID).contains(state.step)) {
				continue;
			}
			stepVisitedVertices.get(vertexID).add(state.getStep());

			if (exitIDs.contains(vertexID)) {
				return composePath(state);
			}

			addNeighbours(step, exitIDs, heuristic, entryID, queue, agentsPerimeter, state, vertexID, maximumDelay);
		}
		return null;
	}

	@NotNull
	private LinkedList<Integer> composePath(State state) {
		LinkedList<Integer> path = new LinkedList<>();
		while (state != null) {
			int id = state.getID();
			path.addFirst(id);

			state = (State) state.getParent();
		}

		return path;
	}

	private void addNeighbours(long step, Set<Integer> exitIDs, Map<Integer, Double> heuristic, int entryID, PriorityQueue<State> queue, double agentPerimeter, State state, int vertexID, int maximumDelay) {
		long stateStep = state.getStep();
		for (int neighbourID : state.getVertex().getNeighbourIDs()) {
			long neighbourStep = stateStep + 1;
			heuristic.computeIfAbsent(neighbourID, k -> getHeuristic(exitIDs, k));
			double estimate;

			if (Double.isFinite(estimate = heuristic.get(neighbourID)) &&
				canVisitVertex(state, entryID, neighbourID, maximumDelay) &&
				(
					stepOccupiedVertices.putIfAbsent(neighbourStep, new HashMap<>()) == null
						|| (
						safeVertex(neighbourStep, neighbourID, agentPerimeter) &&
							safeStepTo(neighbourStep, neighbourID, state.getID(), agentPerimeter)
					)
				) &&
				safeStepFrom(stateStep, state.getID(), neighbourID, agentPerimeter)
			) {
				double distance = graph.getDistance(vertexID, neighbourID);
//				estimate -= 0.015625 * (distance + state.getDistance()) + 0.00390625 * (neighbourStep - step);  // FIXME
				queue.add(new State(state, graph.getVertex(neighbourID), distance, estimate));
			}
		}

		if (
			allowAgentStop &&
				canVisitVertex(state, entryID, vertexID, maximumDelay) &&
				(
					stepOccupiedVertices.putIfAbsent(stateStep + 1, new HashMap<>()) == null || safeVertex(stateStep + 1, vertexID, agentPerimeter)
				)
		) {
			queue.add(new State(state));
		}
	}

	@NotNull
	protected Set<Integer> getExitIDs(Agent agent) {
		if (agent.getExit() >= 0) {
			return Set.of(agent.getExit());
		} else {
			return directionExits.get(agent.getExitDirection());
		}
	}

	protected double getHeuristic(Set<Integer> exitIDs, int vertexID) {
		return exitIDs.stream().mapToDouble(exitID -> graph.getDistance(vertexID, exitID)).min().orElse(0);
	}

	private boolean canVisitVertex(VertexWithDirectionParent state, int entryID, int vertexID, int maximumDelay) {
		if (vertexID == entryID ||
			(
				!allowAgentReturn &&
					state.getParent() != null &&
					state.getParent().getID() == vertexID
			)
		) {
			return false;
		}
		int visitCount = 0;
		int length = 1;
		while (state != null) {
			if (state.getID() == vertexID) {
				visitCount++;
				if (visitCount >= maximumVertexVisits) {
					return false;
				}
			}
			length++;
			state = state.getParent();
		}

		return length <= maximumDelay;
	}

	protected static class State extends VertexWithDirectionParent {
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
