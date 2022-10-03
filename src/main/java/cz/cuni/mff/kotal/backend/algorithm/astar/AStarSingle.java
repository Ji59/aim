package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLines;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirectionParent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class AStarSingle extends SafeLines {
	public static final String MAXIMUM_VERTEX_VISITS_NAME = "Maximum vertex visits";
	public static final int MAXIMUM_VERTEX_VISITS_DEF = 2;
	public static final String ALLOW_AGENT_STOP_NAME = "Allow agent stop";
	public static final boolean ALLOW_AGENT_STOP_DEF = false;
	public static final String MAXIMUM_PATH_DELAY_NAME = "Maximum path delay";
	public static final int MAXIMUM_PATH_DELAY_DEF = Integer.MAX_VALUE;
	public static final String ALLOW_AGENT_RETURN_NAME = "Allow agent to return";
	public static final boolean ALLOW_AGENT_RETURN_DEF = false;

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
		return planAgent(agent, agent.getEntry(), getExitIDs(agent), step);
	}

	@Override
	@Nullable
	public Agent planAgent(Agent agent, int entryID, Set<Integer> exitsIDs, long step) {
		LinkedList<Integer> path = getPath(agent, step, entryID, exitsIDs, Collections.emptyMap());
		if (path == null) {
			return null;
		}

		addPlannedAgent(agent.setPath(path, step));
		return agent;
	}

	@Nullable
	protected LinkedList<Integer> getPath(Agent agent, long step, int entryID, Set<Integer> exitsIDs) {
		return getPath(agent, step, entryID, exitsIDs, Collections.emptyMap());
	}

	@Nullable
	protected LinkedList<Integer> getPath(Agent agent, long step, int entryID, Set<Integer> exitsIDs, Map<Long, Collection<Pair<Integer, Integer>>> constraints) {
		if ((constraints.containsKey(step) && constraints.get(step).stream().anyMatch(c -> c.getVal1() == entryID)) || (stepOccupiedVertices.putIfAbsent(step, new HashMap<>()) != null && !safeVertex(step, entryID, agent.getAgentPerimeter()))) {
			return null;
		}

		long lastStep = getLastStep(agent, exitsIDs, step);

		Map<Integer, Double> heuristic = new HashMap<>();
		double startEstimate = getHeuristic(exitsIDs, entryID);
		heuristic.put(entryID, startEstimate);

		PriorityQueue<State> queue = new PriorityQueue<>();
		queue.add(new State(graph.getVertex(entryID), startEstimate, step));

		return searchPath(agent, exitsIDs, heuristic, entryID, queue, lastStep, constraints);
	}

	protected long getLastStep(Agent agent, Set<Integer> exitsIDs, long step) {
		return step + getMaximumTravelTime(agent, exitsIDs);
	}

	protected int getMaximumTravelTime(Agent agent, Set<Integer> exitsIDs) {
		final int entryID = agent.getEntry();
		return exitsIDs.stream().mapToInt(exitID -> (int) (Math.ceil(graph.getDistance(entryID, exitID)) + maximumPathDelay)).min().getAsInt();
	}

	protected LinkedList<Integer> searchPath(Agent agent, Set<Integer> exitIDs, Map<Integer, Double> heuristic, int entryID, PriorityQueue<State> queue, long lastStep, Map<Long, Collection<Pair<Integer, Integer>>> constraints) {
		double agentsPerimeter = agent.getAgentPerimeter();

		Set<State> visitedStates = new HashSet<>();

		State state;
		while ((state = queue.poll()) != null) {

			if (visitedStates.contains(state) || heuristic.get(state.getID()) < 0) {
				continue;
			}
			visitedStates.add(state);

			final int vertexID = state.getID();

			if (exitIDs.contains(vertexID)) {
				return composePath(state);
			}

			if (state.getStep() < lastStep) {
				addNeighbours(vertexID, entryID, exitIDs, state, queue, heuristic, agentsPerimeter, lastStep, visitedStates, constraints.get(state.getStep() + 1));
			}
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

	private void addNeighbours(int vertexID, int entryID, Set<Integer> exitIDs, State state, PriorityQueue<State> queue, Map<Integer, Double> heuristic, double agentPerimeter, long lastStep, Set<State> visitedStates, @Nullable Collection<Pair<Integer, Integer>> constraints) {
		final long stateStep = state.getStep();
		final long neighbourStep = stateStep + 1;
		for (int neighbourID : state.getVertex().getNeighbourIDs()) {
			if (visitedStates.contains(new State(state, graph.getVertex(neighbourID), 0, 0))) {
				continue;
			}

			heuristic.computeIfAbsent(neighbourID, k -> getHeuristic(exitIDs, k));
			double estimate = heuristic.get(neighbourID);

			if (Double.isFinite(estimate) && neighbourStep + estimate <= lastStep &&
				canVisitVertex(state, entryID, neighbourID, constraints) &&
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
				final State neighbourState = new State(state, graph.getVertex(neighbourID), distance, estimate);
				queue.add(neighbourState);

			}
		}

		if (
			allowAgentStop &&
				canVisitVertex(state, entryID, vertexID, constraints) &&
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

	private boolean canVisitVertex(final VertexWithDirectionParent state, int entryID, int vertexID, @Nullable Collection<Pair<Integer, Integer>> constraints) {
		if ((state.getParent() == null && vertexID == entryID) ||
			(
				!allowAgentReturn &&
					state.getParent() != null &&
					state.getParent().getID() == vertexID
			) ||
			(constraints != null && constraints.stream()
				.anyMatch(c -> {
					int actualID = c.getVal1();
					if (actualID != vertexID) {
						return false;
					}

					int lastID = c.getVal0();
					if (lastID == actualID) {
						return true;
					}
					return state.getID() == lastID;
				}))
		) {
			return false;
		}
		int visitCount = 0;
		VertexWithDirectionParent parent = state;
		while (parent != null) {
			if (parent.getID() == vertexID) {
				visitCount++;
				if (visitCount >= maximumVertexVisits) {
					return false;
				}
			}
			parent = parent.getParent();
		}

		return true;
	}

	protected class State extends VertexWithDirectionParent {
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof State state)) return false;
			if (!super.equals(o)) return false;

			if (step == state.step) {
				if (!allowAgentReturn) {
					VertexWithDirectionParent parent = getParent();
					if (parent != null) {
						VertexWithDirectionParent stateParent = state.getParent();
						assert stateParent != null;
						assert (parent.getID() == stateParent.getID()) == (angle == state.angle);
						return parent.getID() == stateParent.getID();
					}
					assert state.getParent() == null;
				}

				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (int) (step ^ (step >>> 32));
			return result;
		}
	}
}
