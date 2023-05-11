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


/**
 * Implementation of A* algorithm planning each agent alone
 */
public class AStarSingle extends SafeLanes {
	/**
	 * Parameters names and default values
	 */
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

	/**
	 * Create new A* planner and load parameter values.
	 *
	 * @param graph Graph in which paths are searched
	 */
	public AStarSingle(@NotNull SimulationGraph graph) {
		super(graph);

		maximumVertexVisits = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		allowAgentStop = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
		maximumPathDelay = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_PATH_DELAY_NAME, MAXIMUM_PATH_DELAY_DEF);
		allowAgentReturn = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_RETURN_NAME, ALLOW_AGENT_RETURN_DEF);
	}

	/**
	 * Create new A* planner with specific parameters.
	 *
	 * @param graph               Graph in which paths are searched
	 * @param safeDistance        value for safe distance parameter
	 * @param maximumVertexVisits value for maximum vertex visits parameter
	 * @param allowAgentStop      true if agents are allowed to stop on vertices
	 * @param maximumPathDelay    value for maximum path delay parameter
	 * @param allowAgentReturn    true if agents are allowed to return to last visited vertex
	 */
	@TestOnly
	protected AStarSingle(@NotNull SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn) {
		super(graph, safeDistance);
		this.maximumVertexVisits = maximumVertexVisits;
		this.allowAgentStop = allowAgentStop;
		this.maximumPathDelay = maximumPathDelay;
		this.allowAgentReturn = allowAgentReturn;
	}

	/**
	 * Try to plan agent.
	 *
	 * @param agent Agent to be planned
	 * @param step  Number of step in which agents should be planned
	 * @return Agent if planning was successful, otherwise null
	 */
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

	/**
	 * Try to find a path for the agent starting at the specified step.
	 *
	 * @param agent    Agent to be planned
	 * @param step     Step when planning started
	 * @param entryID  ID of entry vertex
	 * @param exitsIDs IDs of valid exits
	 * @return Valid path if found, otherwise null
	 */
	@Nullable
	protected List<Integer> getPath(@NotNull Agent agent, long step, int entryID, @NotNull Set<Integer> exitsIDs) {
		return getPath(agent, step, entryID, exitsIDs, Collections.emptyMap());
	}

	/**
	 * Try to find a path for the agent starting at the specified step while avoiding prohibited transfers.
	 *
	 * @param agent       Agent to be planned
	 * @param step        Step when planning started
	 * @param entryID     ID of entry vertex
	 * @param exitsIDs    IDs of valid exits
	 * @param constraints Constraints on agent movement consisting of map between step and a pair of prohibited transfers
	 * @return Valid path if found, otherwise null
	 */
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

	/**
	 * @return Step to which agent has to arrive at any exit
	 */
	protected long getLastStep(@NotNull Agent agent, @NotNull Set<Integer> exitsIDs, long step) {
		return step + getMaximumTravelTime(agent, exitsIDs);
	}

	/**
	 * @return Maximum number of steps in which the agent can achieve any exit computed from optimal travel
	 */
	protected int getMaximumTravelTime(@NotNull Agent agent, @NotNull Set<Integer> exitsIDs) {
		final int entryID = agent.getEntry();
		return exitsIDs.stream().mapToInt(exitID -> (int) (Math.ceil(graph.getDistance(entryID, exitID)))).min().getAsInt() + maximumPathDelay;
	}

	/**
	 * Try to find the shortest path for agent to any exit according to heuristics.
	 *
	 * @param agent       Agent, for whom the path is planned
	 * @param exitIDs     Ids of valid exits
	 * @param heuristic   Estimate on distances to nearest exit
	 * @param entryID     ID of entry vertex
	 * @param queue       Priority Queue for states
	 * @param lastStep    Last step when agent can reach exit
	 * @param constraints Constraints on agent movement consisting of map between step and a pair of prohibited transfers
	 * @return Valid path if found, otherwise null
	 */
	protected @Nullable List<Integer> searchPath(@NotNull Agent agent, @NotNull Set<Integer> exitIDs, @NotNull Map<Integer, Double> heuristic, int entryID, @NotNull PriorityQueue<State> queue, long lastStep, @NotNull Map<Long, Collection<Pair<Integer, Integer>>> constraints) {
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
				addNeighbours(entryID, exitIDs, state, queue, heuristic, agentsPerimeter, lastStep, visitedStates, constraints.get(state.getStep() + 1));
			}
		}
		return null;
	}

	@NotNull
	private List<Integer> composePath(@Nullable State state) {
		@NotNull LinkedList<Integer> path = new LinkedList<>();
		while (state != null) {
			int id = state.getID();
			path.addFirst(id);

			state = (State) state.getParent();
		}

		return new ArrayList<>(path);
	}

	/**
	 * Add all valid neighbour states to the queue.
	 *
	 * @param entryID        ID of entry vertex
	 * @param exitIDs        IDs of exit vertices
	 * @param state          Last state
	 * @param queue          Queue of states
	 * @param heuristic      Estimate to nearest exit
	 * @param agentPerimeter Perimeter of the agent
	 * @param lastStep       Maximum step of travel
	 * @param visitedStates  Processed states
	 * @param constraints    Constraints on agent movement
	 */
	protected void addNeighbours(int entryID, @NotNull Set<Integer> exitIDs, @NotNull State state, @NotNull PriorityQueue<State> queue, @NotNull Map<Integer, Double> heuristic, double agentPerimeter, long lastStep, @NotNull Set<State> visitedStates, @Nullable Collection<Pair<Integer, Integer>> constraints) {
		final int vertexID = state.getID();
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

	/**
	 * Check parent, constraints and number of vertex visits to determine, if vertex visit is valid.
	 *
	 * @param state       Last state
	 * @param entryID     Ignored
	 * @param vertexID    ID of the next vertex
	 * @param constraints Collection of prohibited transfers in the same step
	 * @return True if the vertex can be visited in next step
	 */
	protected boolean canVisitVertex(final @NotNull State state, int entryID, int vertexID, @Nullable Collection<Pair<Integer, Integer>> constraints) {
		return validParent(state, vertexID) && safeConstraints(state, vertexID, constraints) && validVisitCount(state, vertexID, maximumVertexVisits);
	}

	/**
	 * Check if parent vertex is not same when returning to last vertex is not allowed.
	 *
	 * @param state    Last state
	 * @param vertexID ID of the next vertex
	 * @return True if state or its parent contains different vertex, otherwise false
	 */
	protected boolean validParent(@NotNull VertexWithDirectionParent state, int vertexID) {
		return (state.getParent() != null || vertexID != state.getID()) && // agent cannot stop at starting vertex
			(
				allowAgentReturn ||
					state.getParent() == null ||
					state.getParent().getID() != vertexID
			);
	}

	/**
	 * Check if number of visits of vertex is within limit.
	 *
	 * @param state               Last state
	 * @param vertexID            ID of the next vertex
	 * @param maximumVertexVisits Maximum allowed number of vertex visits
	 * @return False if vertex was visited too often, otherwise true
	 */
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

	/**
	 * Check if the transfer between state and new vertex isn't contained in constraint.
	 *
	 * @param state       Last state
	 * @param vertexID    New vertex ID
	 * @param constraints Collection of prohibited transfers in the same step
	 * @return True if transfer is not in constraints, otherwise false
	 */
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

	/**
	 * @return Set of vertex IDs that are valid exits for specified agent
	 */
	@NotNull
	protected Set<Integer> getExitIDs(@NotNull Agent agent) {
		if (agent.getExit() >= 0) {
			return Set.of(agent.getExit());
		} else {
			return directionExits.get(agent.getExitDirection());
		}
	}

	/**
	 * Class extending {@link VertexWithDirectionParent} containing also number of state step and remembering step of last stop along path to this state.
	 */
	protected class State extends VertexWithDirectionParent {
		private final long step;
		private final long lastStop;

		/**
		 * Create new object with specified values, zero distance and null parent.
		 * Last stop is set to {@code Long.MAX_VALUE}.
		 *
		 * @param vertex   Vertex this object is linked to
		 * @param estimate Estimate to goal
		 * @param step     Step of the state
		 */
		public State(GraphicalVertex vertex, double estimate, long step) {
			super(vertex, 0, estimate);
			this.step = step;
			this.lastStop = Long.MAX_VALUE;
		}

		/**
		 * Create new object according to arguments.
		 * Distance is taken from state adn then add 1.
		 * Finally, compute step and last stop.
		 * <p>
		 * This constructor should be used when agent stays on one vertex.
		 *
		 * @param state Last Vertex with safe distance and direction
		 */
		public State(@NotNull State state) {
			super(state, state.getVertex(), 1, state.getEstimate());
			this.step = state.getStep() + 1;
			this.lastStop = step;
		}

		/**
		 * Create new object according to arguments.
		 * Distance is taken from parent and then add distance between vertices from argument.
		 * Then compute angle from previous vertex to actual and set parent to previous vertex.
		 * Finally, compute step and last stop.
		 *
		 * @param previous Last state with safe distance and direction
		 * @param actual   New vertex on path
		 * @param distance Distance between those vertices
		 * @param estimate Estimate to goal
		 */
		public State(@NotNull State previous, GraphicalVertex actual, double distance, double estimate) {
			super(previous, actual, distance, estimate);
			this.step = previous.getStep() + 1;
			this.lastStop = previous.vertex.equals(actual) ? step : previous.lastStop;
		}

		public long getStep() {
			return step;
		}

		/**
		 * Compare this vertex with argument.
		 * If the argument is not {@link State}, return false.
		 * If the argument not equal according to {@link VertexWithDirectionParent}, also return false.
		 * Else compare parents of the state.
		 *
		 * @param o the object to be compared
		 * @return True if the object represents same state
		 */
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
		 * Compare vertices according to {@link VertexWithDirection}.
		 * If the comparison is zero, compare step, when the path stopped last time.
		 * This should cause to path to prefer stopping at latter moment.
		 *
		 * @param o the object to be compared.
		 * @return Same value as parent if the value is nonzero, otherwise comparison between last stop
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
