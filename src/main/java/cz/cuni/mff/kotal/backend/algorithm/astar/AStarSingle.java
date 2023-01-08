package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLanes;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirection;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirectionParent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class AStarSingle extends SafeLanes {
	public static final String MAXIMUM_VERTEX_VISITS_NAME = "Maximum vertex visits";
	public static final int MAXIMUM_VERTEX_VISITS_DEF = 2;
	public static final String ALLOW_AGENT_STOP_NAME = "Allow agent stop";
	public static final boolean ALLOW_AGENT_STOP_DEF = false;
	public static final String MAXIMUM_PATH_DELAY_NAME = "Maximum path delay";
	public static final int MAXIMUM_PATH_DELAY_DEF = Integer.MAX_VALUE;
	public static final String ALLOW_AGENT_RETURN_NAME = "Allow agent to return";
	public static final boolean ALLOW_AGENT_RETURN_DEF = false;

	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SafeLanes.PARAMETERS);

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

	public AStarSingle(@NotNull SimulationGraph graph) {
		super(graph);

		maximumVertexVisits = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		allowAgentStop = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
		maximumPathDelay = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_PATH_DELAY_NAME, MAXIMUM_PATH_DELAY_DEF);
		allowAgentReturn = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_RETURN_NAME, ALLOW_AGENT_RETURN_DEF);
	}

	@TestOnly
	protected AStarSingle(@NotNull SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn) {
		super(graph, safeDistance);
		this.maximumVertexVisits = maximumVertexVisits;
		this.allowAgentStop = allowAgentStop;
		this.maximumPathDelay = maximumPathDelay;
		this.allowAgentReturn = allowAgentReturn;
	}

	@Override
	public @Nullable Agent planAgent(@NotNull Agent agent, long step) {
		return planAgent(agent, agent.getEntry(), getExitIDs(agent), step);
	}

	@Override
	@Nullable
	public Agent planAgent(@NotNull Agent agent, int entryID, @NotNull Set<Integer> exitsIDs, long step) {
		if (exitsIDs.isEmpty()) {
			exitsIDs = getExits(agent);
		}
		@Nullable List<Integer> path = getPath(agent, step, entryID, exitsIDs, Collections.emptyMap());
		if (path == null) {
			return null;
		}

		addPlannedAgent(agent.setPath(path, step));
		return agent;
	}

	@Nullable
	protected List<Integer> getPath(@NotNull Agent agent, long step, int entryID, @NotNull Set<Integer> exitsIDs, @NotNull Map<Long, Collection<Pair<Integer, Integer>>> constraints) {
		if ((constraints.containsKey(step) && constraints.get(step).stream().anyMatch(c -> c.getVal1() == entryID)) || (stepOccupiedVertices.containsKey(step) && stepOccupiedVertices.get(step).containsKey(entryID)) || stopped) {
			return null;
		}

		long lastStep = getLastStep(agent, exitsIDs, step);

		@NotNull Map<Integer, Double> heuristic = new HashMap<>();
		double startEstimate = getHeuristic(exitsIDs, entryID);
		heuristic.put(entryID, startEstimate);

		@NotNull PriorityQueue<State> queue = new PriorityQueue<>();
		queue.add(new State(graph.getVertex(entryID), startEstimate, step));

		return searchPath(agent, exitsIDs, heuristic, entryID, queue, lastStep, constraints);
	}

	protected long getLastStep(@NotNull Agent agent, @NotNull Set<Integer> exitsIDs, long step) {
		return step + getMaximumTravelTime(agent, exitsIDs);
	}

	protected int getMaximumTravelTime(@NotNull Agent agent, @NotNull Set<Integer> exitsIDs) {
		final int entryID = agent.getEntry();
		return exitsIDs.stream().mapToInt(exitID -> (int) (Math.ceil(graph.getDistance(entryID, exitID)) + maximumPathDelay)).min().getAsInt();
	}

	protected @Nullable LinkedList<Integer> searchPath(@NotNull Agent agent, @NotNull Set<Integer> exitIDs, @NotNull Map<Integer, Double> heuristic, int entryID, @NotNull PriorityQueue<State> queue, long lastStep, @NotNull Map<Long, Collection<Pair<Integer, Integer>>> constraints) {
		double agentsPerimeter = agent.getAgentPerimeter();

		@NotNull Set<State> visitedStates = new HashSet<>();

		State state;
		while ((state = queue.poll()) != null && !stopped) {

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
	private LinkedList<Integer> composePath(@Nullable State state) {
		@NotNull LinkedList<Integer> path = new LinkedList<>();
		while (state != null) {
			int id = state.getID();
			path.addFirst(id);

			state = (State) state.getParent();
		}

		return path;
	}

	protected void addNeighbours(int vertexID, int entryID, @NotNull Set<Integer> exitIDs, @NotNull State state, @NotNull PriorityQueue<State> queue, @NotNull Map<Integer, Double> heuristic, double agentPerimeter, long lastStep, @NotNull Set<State> visitedStates, @Nullable Collection<Pair<Integer, Integer>> constraints) {
		final long stateStep = state.getStep();
		final long neighbourStep = stateStep + 1;
		for (int neighbourID : state.getVertex().getNeighbourIDs()) {
			if (visitedStates.contains(new State(state, graph.getVertex(neighbourID), 0, 0))) {
				continue;
			}

			heuristic.computeIfAbsent(neighbourID, k -> getHeuristic(exitIDs, k));
			double estimate = heuristic.get(neighbourID);

			if (Double.isFinite(estimate) && neighbourStep + estimate <= lastStep &&
				canVisitVertex(state, entryID, neighbourID, constraints) && safeStepTo(stateStep, state.getID(), neighbourID, agentPerimeter)
			) {
				double distance = graph.getDistance(vertexID, neighbourID);
				final @NotNull State neighbourState = new State(state, graph.getVertex(neighbourID), distance, estimate);
				queue.add(neighbourState);

			}
		}

		if (allowAgentStop && canVisitVertex(state, entryID, vertexID, constraints) && safeStepTo(stateStep, vertexID, vertexID, agentPerimeter)) {
			queue.add(new State(state));
		}
	}

	protected boolean canVisitVertex(final @NotNull State state, int entryID, int vertexID, @Nullable Collection<Pair<Integer, Integer>> constraints) {
		return validParent(state, vertexID) && safeConstraints(state, vertexID, constraints) && validVisitCount(state, vertexID, maximumVertexVisits);
	}

	protected boolean validParent(@NotNull VertexWithDirectionParent state, int vertexID) {
		return (state.getParent() != null || vertexID != state.getID()) && // agent cannot stop at starting vertex
			(
				allowAgentReturn ||
					state.getParent() == null ||
					state.getParent().getID() != vertexID
			);
	}

	protected boolean validVisitCount(@NotNull VertexWithDirectionParent state, int vertexID, int maximumVertexVisits) {
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

	protected boolean safeConstraints(@NotNull VertexWithDirectionParent state, int vertexID, @Nullable Collection<Pair<Integer, Integer>> constraints) {
		return constraints == null || constraints.stream()
			.allMatch(c -> {
				final int actualID = c.getVal1();
				if (actualID != vertexID) {
					return true;
				}

				final int lastID = c.getVal0();
				return lastID != actualID && state.getID() != lastID;
			});
	}

	protected double getHeuristic(@NotNull Set<Integer> exitIDs, int vertexID) {
		return exitIDs.stream().mapToDouble(exitID -> graph.getDistance(vertexID, exitID)).min().orElse(0);
	}

	@NotNull
	protected Set<Integer> getExitIDs(@NotNull Agent agent) {
		if (agent.getExit() >= 0) {
			return Set.of(agent.getExit());
		} else {
			return directionExits.get(agent.getExitDirection());
		}
	}

	@Nullable
	protected List<Integer> getPath(@NotNull Agent agent, long step, int entryID, @NotNull Set<Integer> exitsIDs) {
		return getPath(agent, step, entryID, exitsIDs, Collections.emptyMap());
	}

	protected class State extends VertexWithDirectionParent {
		private final long step;
		private final long lastStop;

		public State(GraphicalVertex vertex, double estimate, long step) {
			super(vertex, 0, estimate);
			this.step = step;
			this.lastStop = Long.MAX_VALUE;
		}

		public State(@NotNull State state) {
			super(state, state.getVertex(), 1, state.getEstimate());
			this.step = state.getStep() + 1;
			this.lastStop = step;
		}

		public long getStep() {
			return step;
		}

		public State(@NotNull State previous, GraphicalVertex actual, double distance, double estimate) {
			super(previous, actual, distance, estimate);
			this.step = previous.getStep() + 1;
			this.lastStop = previous.vertex.equals(actual) ? step : previous.lastStop;
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
						assert allowAgentStop || (parent.getID() == stateParent.getID()) == (angle == state.angle);
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

		/**
		 * @param o the object to be compared.
		 * @return
		 */
		@Override
		public int compareTo(@NotNull VertexWithDirection o) {
			final int superComparison = super.compareTo(o);
			if (superComparison == 0 && o instanceof State s) {
				return Long.compare(s.lastStop, lastStop);
			}
			return superComparison;
		}
	}
}
