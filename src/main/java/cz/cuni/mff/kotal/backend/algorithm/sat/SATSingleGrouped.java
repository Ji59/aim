package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLanes;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.maxsat.WeightedPartialMaxsat;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle.*;
import static cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2.getBooleanParameter;
import static cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2.getIntegerParameter;

public class SATSingleGrouped extends SafeLanes {
	/**
	 * Parameters names and default values
	 */
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SafeLanes.PARAMETERS);
	protected static final String MAXIMUM_STEPS_NAME = "Maximum simulation steps";
	protected static final int MAXIMUM_STEPS_DEF = 64;
	protected static final String OPTIMIZED_NAME = "Optimize paths";
	protected static final boolean OPTIMIZED_DEF = true;

	static {
		PARAMETERS.put(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		PARAMETERS.put(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);

		PARAMETERS.put(MAXIMUM_STEPS_NAME, MAXIMUM_STEPS_DEF);
		PARAMETERS.put(OPTIMIZED_NAME, OPTIMIZED_DEF);
	}

	protected final int maximumVertexVisits;
	protected final boolean allowAgentStop;
	protected final int maximumSteps;
	protected final boolean optimized;
	protected final List<Integer>[] inverseNeighbours = new List[graph.getVertices().length];
	protected WeightedPartialMaxsat solver;

	public SATSingleGrouped(@NotNull SimulationGraph graph) {
		super(graph);
		maximumVertexVisits = getIntegerParameter(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		allowAgentStop = getBooleanParameter(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
		maximumSteps = getIntegerParameter(MAXIMUM_STEPS_NAME, MAXIMUM_STEPS_DEF);
		optimized = getBooleanParameter(OPTIMIZED_NAME, OPTIMIZED_DEF);

		for (int i = 0; i < graph.getVertices().length; i++) {
			inverseNeighbours[i] = new LinkedList<>();
		}
		for (@NotNull Vertex vertex : graph.getVertices()) {
			for (int neighbourID : vertex.getNeighbourIDs()) {
				inverseNeighbours[neighbourID].add(vertex.getID());
			}
		}
	}

	@Override
	public Collection<Agent> planAgents(@NotNull Collection<Agent> agents, final long step) {
		filterStepOccupiedVertices(step);

		return planAgents(
			agents.stream()
				.collect(Collectors.toMap(
					Function.identity(),
					a -> new Pair<>(
						a.getEntry(),
						getExits(a)
					)
				)),
			step
		);
	}

	@Override
	public Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		if (agentsEntriesExits.isEmpty()) {
			return agentsEntriesExits.keySet();
		}

		final IPBSolver ipbSolver = optimized ? SolverFactory.newDefaultOptimizer() : SolverFactory.newDefault();
		final @NotNull WeightedPartialMaxsat solver = new WeightedMaxSatDecorator(ipbSolver, false);

		final int agentsCount = agentsEntriesExits.size();
		final int vertices = graph.getVertices().length;

		final @NotNull Deque<Triplet<Integer, Agent, boolean[][]>> offsets = new ArrayDeque<>(agentsCount);

		createAgentsClauses(agentsEntriesExits, step, solver, vertices, offsets);

		final @NotNull Set<Agent> plannedAgents = new HashSet<>(agentsCount);
		try {
			if (solver.isSatisfiable()) {
				int[] model = solver.model();
				for (@NotNull Triplet<Integer, Agent, boolean[][]> agentEntry : offsets) {
					transferAgentPath(agentsEntriesExits, step, plannedAgents, model, agentEntry);
				}
			} else {
				assert false;
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
		}

		return plannedAgents;
	}

	private void createAgentsClauses(final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, final long step, final @NotNull WeightedPartialMaxsat solver, final int vertices, final @NotNull Deque<Triplet<Integer, Agent, boolean[][]>> offsets) {
		final int agentsCount = agentsEntriesExits.size();

		solver.newVar(agentsCount * (vertices * (maximumSteps + 1)) + 1);
		int offset = 1;
		for (Map.@NotNull Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExit : agentsEntriesExits.entrySet()) {
			if (stopped) {
				return;
			}
			final Agent agent = agentEntryExit.getKey();
			final int entry = agentEntryExit.getValue().getVal0();
			if (stepOccupiedVertices.get(step).containsKey(entry)) {
				continue;
			}
			Set<Integer> exits = agentEntryExit.getValue().getVal1();

			boolean[] @NotNull [] validTimeVertices = getValidTimeVertices(entry, exits, step, agent.getAgentPerimeter());

			offsets.add(new Triplet<>(offset, agent, validTimeVertices));
			try {
				createAgentClauses(solver, entry, exits, offsets, step, agentsCount);
				offset += vertices * (maximumSteps + 1);
			} catch (ContradictionException e) {
				e.printStackTrace();
				offsets.removeLast();
			}
		}
	}

	/**
	 * Agent is on vertex `v_i` in time (step + t) iff  |V| * t + i + 1 is set
	 * TODO
	 *
	 * @param solver
	 * @param startingVertex
	 * @param exits
	 * @param offsets
	 * @param step
	 * @param agentsCount
	 * @return
	 */
	protected void createAgentClauses(@NotNull WeightedPartialMaxsat solver, int startingVertex, @NotNull Set<Integer> exits, @NotNull Deque<Triplet<Integer, Agent, boolean[][]>> offsets, long step, int agentsCount) throws ContradictionException {
		Triplet<Integer, Agent, boolean[][]> offsetAgent = offsets.getLast();
		final Agent agent = offsetAgent.getVal1();
		final double agentPerimeter = agent.getAgentPerimeter();

		final int vertices = graph.getVertices().length;
		final int offset = offsetAgent.getVal0();

		addAgentsClauses(solver, offsets, step);

		addExitsClause(solver, startingVertex, exits, offset, agent);

		final boolean[][] validTimeVertices = offsetAgent.getVal2();

		/*
			Restrict agent from visiting vertex more than specified
		 */
		final List<Integer> @NotNull [] validLits = new LinkedList[vertices];
		for (int i = 0; i < vertices; i++) {
			validLits[i] = new LinkedList<>();
		}
		for (int s = 0; s <= maximumSteps; s++) {
			final int stepOffset = offset + s * vertices;
			for (int i = 0; i < vertices; i++) {
				if (validTimeVertices[s][i]) {
					validLits[i].add(stepOffset + i);
				}
			}
		}
		for (int i = 0; i < vertices; i++) {
			final List<Integer> vertexLits = validLits[i];
			if (vertexLits != null) {
				solver.addAtMost(VecInt.of(vertexLits), maximumVertexVisits);
			}
		}

		for (int t = 0; t <= maximumSteps; t++) {
			final int timeOffset = vertices * t + offset;

			@NotNull List<Integer> validVerticesIDs = new LinkedList<>();
			for (int i = 0; i < vertices; i++) {
				if (validTimeVertices[t][i]) {
					validVerticesIDs.add(i);
				} else {
					/*
						agent cannot be at vertex v_i before the shortest path from start and after the shortest path to any goal
					*/
					solver.addHardClause(VecInt.of(-(timeOffset + i)));
				}
			}

			/*
				if agent is at v_i in time t, he is not in v_j != v_i
				=> (|V| * t + i + offset) -> (∧_{vertex v_j, i != j} ¬(|V| * t + j + ))
				<=> ∀_{vertex j != i} - (|V| * t + i + offset) ∨ - (|V| * t + j + offset)
				<=> ∀_{vertex j < i} - (|V| * t + i + offset) ∨ - (|V| * t + j + offset)
			*/

			solver.addAtMost(VecInt.of(validVerticesIDs.stream().map(id -> timeOffset + id).toList()), 1);


			if (t < maximumSteps) {
				/*
					Relations between time t and (t + 1)
				*/
				final long tStep = step + t;
				final int nextTimeOffset = timeOffset + vertices;

				for (int i : validVerticesIDs) {
					if (!exits.contains(i)) {
						/* FIXME it changed :(
							∀ vertex v_i different from exit in time t if |V| * t + i + 1 (agent is on v_i in t) then agent has to be on any neighbour in next time
							=> ¬(|V| * t + i + offset) = - (|V| * t + i + offset) ∨ (V_{neighbour j of v_i} |V| * (t + 1) + j + offset)
						*/
						@NotNull Set<Integer> validNeighbours = graph.getVertex(i).getNeighbourIDs();
						if (allowAgentStop && i != startingVertex) {
							validNeighbours.add(i);
						}
						validNeighbours = validNeighbours.stream()
							.filter(id -> safeStepTo(tStep, i, id, agentPerimeter))
							.collect(Collectors.toSet());
						if (validNeighbours.isEmpty()) {
							/*
								agent can not be in vertex which he can not get out of
							*/
							solver.addHardClause(VecInt.of(-(timeOffset + i)));
						} else {
							final @NotNull IVecInt clause = new VecInt(validNeighbours.stream().mapToInt(id -> nextTimeOffset + id).toArray());
							clause.push(-(timeOffset + i));
							solver.addHardClause(clause);
						}
					}
				}
			}
		}
	}

	protected void addAgentsClauses(@NotNull WeightedPartialMaxsat solver, @NotNull Deque<Triplet<Integer, Agent, boolean[][]>> offsets, long step) throws ContradictionException {
		final int vertices = graph.getVertices().length;
		Triplet<Integer, Agent, boolean[][]> last = offsets.getLast();
		final int agentOffset = last.getVal0();
		final double agentPerimeter = last.getVal1().getAgentPerimeter();
		final boolean[][] validTimeVertices = last.getVal2();

		for (int i = 0; i < vertices; i++) {
			addAgentsVertexClauses(solver, offsets, vertices, agentOffset, agentPerimeter, validTimeVertices, i);
		}

	}

	private void addAgentsVertexClauses(@NotNull WeightedPartialMaxsat solver, @NotNull Deque<Triplet<Integer, Agent, boolean[][]>> offsets, int vertices, int agentOffset, double agentPerimeter, boolean[][] validTimeVertices, int i) throws ContradictionException {
    /*
			if agent `a_i` is at vertex `v_x` at time `t`, no other agent `n` can be at near vertex `v_y` at time `t`.
			=> ∀_{vertex x, time t} ((|V| * t + x + offset_i) -> ∧_{agent j != i, vertex y near x} ¬(|V| * t + y + offset_j))
			<=> ∀_{vertex x, time t} ∀_{agent j != i} ∀_{vertex y near x} (¬(|V| * t + x + offset_i) ∨ ¬(|V| * t + y + offset_j))
		*/
		for (@NotNull Triplet<Integer, Agent, boolean[][]> offsetTriplet : offsets) {
			final int neighbourOffset = offsetTriplet.getVal0();
			if (neighbourOffset == agentOffset) {
				continue;
			}

			final double neighbourPerimeter = offsetTriplet.getVal1().getAgentPerimeter();
			final boolean[][] neighbourValidTimeVertices = offsetTriplet.getVal2();
			final double safePerimeter = agentPerimeter + neighbourPerimeter + safeDistance;
			addAgentsNearVerticesClauses(solver, vertices, agentOffset, safePerimeter, validTimeVertices, i, neighbourOffset, neighbourValidTimeVertices);

			for (int neighbourID : graph.getVertex(i).getNeighbourIDs()) {
				addAgentsTravellingClauses(solver, vertices, agentOffset, safePerimeter, validTimeVertices, i, neighbourID, neighbourOffset, neighbourValidTimeVertices);
			}
			if (allowAgentStop) {
				addAgentsTravellingClauses(solver, vertices, agentOffset, safePerimeter, validTimeVertices, i, i, neighbourOffset, neighbourValidTimeVertices);
			}
		}
	}

	private void addAgentsNearVerticesClauses(@NotNull WeightedPartialMaxsat solver, int vertices, int agentOffset, double agentsPerimeter, boolean[][] validTimeVertices, int vertexID, int neighbourOffset, boolean[][] neighbourValidTimeVertices) throws ContradictionException {
		for (int conflictVertexID : conflictVertices(vertexID, agentsPerimeter)) {
			for (int t = 0, timeOffset = 0; t <= maximumSteps; t++, timeOffset += vertices) {
				if (validTimeVertices[t][vertexID] && neighbourValidTimeVertices[t][conflictVertexID]) {
					solver.addHardClause(new VecInt(new int[]{-(timeOffset + vertexID + agentOffset), -(timeOffset + conflictVertexID + neighbourOffset)}));
				}
			}
		}
	}

	private void addAgentsTravellingClauses(@NotNull WeightedPartialMaxsat solver, int vertices, int agent0Offset, double safePerimeter, boolean[][] validTimeVertices, int vertex0ID, int nextVertex0ID, int agent1Offset, boolean[][] agent1ValidTimeVertices) throws ContradictionException {
		@NotNull final Map<Integer, Double>[] agent1TravelDistances = graph.getTravelDistances()[vertex0ID].get(nextVertex0ID);
		for (SimulationGraph.VertexDistance vertexDistance : graph.getSortedVertexVerticesTravelDistances()[vertex0ID].get(nextVertex0ID)) {
			if (safePerimeter < vertexDistance.distance()) {
				break;
			}

			final int vertex1ID = vertexDistance.vertexID();
			@NotNull final Map<Integer, Double> agent1AgentTravelDistance = agent1TravelDistances[vertex1ID];
			for (int nextVertex1ID : graph.getVertex(vertex1ID).getNeighbourIDs()) {
				addAgentsTravellingClause(solver, vertices, safePerimeter, validTimeVertices, vertex0ID, agent0Offset, nextVertex0ID, agent1ValidTimeVertices, agent1Offset, vertex1ID, nextVertex1ID, agent1AgentTravelDistance);
			}
			if (allowAgentStop) {
				addAgentsTravellingClause(solver, vertices, safePerimeter, validTimeVertices, vertex0ID, agent0Offset, nextVertex0ID, agent1ValidTimeVertices, agent1Offset, vertex1ID, vertex1ID, agent1AgentTravelDistance);
			}
		}
	}

	private void addAgentsTravellingClause(@NotNull WeightedPartialMaxsat solver, int vertices, double safePerimeter, boolean[][] validTimeVertices, int vertex0ID, int agent0Offset, int nextVertex0ID, boolean[][] agent1ValidTimeVertices, int agent1Offset, int vertex1ID, int nextVertex1ID, Map<Integer, Double> agent1AgentTravelDistance) throws ContradictionException {
		/*
			if agent `a` is going from `v_i` to `v_j` at time `t` and previous agent `b` is going from `v_p` to `v_q` at time `t`
			and they would collide, agent `a` can not be at `v_i` at `t` or agent `a` can not be at `v_j` at `(t + 1)`
			or agent `b` can not be at `v_p` at `t` or agent `b` can not be at `v_q` at `(t + 1)`
			=> ¬(|V| * t + i + offset_a) ∨ ¬(|V| * (t + 1) + j + offset_a) ∨ ¬(|V| * t + p + offset_b) ∨ ¬(|V| * (t + 1) + q + offset_b)
		*/
		if (safePerimeter >= agent1AgentTravelDistance.get(nextVertex1ID)) {
			for (int t = 0, timeOffset = 0, nextTimeOffset = vertices; t < maximumSteps; t++, timeOffset += vertices, nextTimeOffset += vertices) {
				if (validTimeVertices[t][vertex0ID] && validTimeVertices[t + 1][nextVertex0ID] &&
					agent1ValidTimeVertices[t][vertex1ID] && agent1ValidTimeVertices[t + 1][nextVertex1ID]) {
					solver.addHardClause(VecInt.of(
						-(timeOffset + vertex0ID + agent0Offset),
						-(nextTimeOffset + nextVertex0ID + agent0Offset),
						-(timeOffset + vertex1ID + agent1Offset),
						-(nextTimeOffset + nextVertex1ID + agent1Offset)
					));
				}
			}
		}
	}

	/**
	 * Agent can be at one exit over all steps, so exactly one is true from ∪_{i>=1} { offset + vertices * i + e | e ∈ exits }.
	 * If agent cannot be planned, (offset + startingVertex) is false.
	 * <p>
	 * Add soft constraints to maximize planned agents and minimize travel times.
	 *
	 * @param solver
	 * @param startingVertex
	 * @param exits
	 * @param offset
	 * @throws ContradictionException
	 */
	protected void addExitsClause(final @NotNull WeightedPartialMaxsat solver, final int startingVertex, final @NotNull Set<Integer> exits, final int offset, final Agent agent) throws ContradictionException {
		final int vertices = graph.getVertices().length;
		final int startingVertexOffset = startingVertex + offset;
		final boolean addClause = addNoValidRouteClause(solver, startingVertexOffset, agent);
		final int @NotNull [] entryExitsLits;
		final int startingIndex;
		if (addClause) {
			entryExitsLits = new int[maximumSteps * exits.size() + 1];
			entryExitsLits[0] = -startingVertexOffset;
			startingIndex = 1;
		} else {
			entryExitsLits = new int[maximumSteps * exits.size()];
			startingIndex = 0;
		}

		final @NotNull Iterator<Integer> exitsIt = exits.iterator();
		for (int i = 0; exitsIt.hasNext(); i += maximumSteps) {
			final int exit = exitsIt.next();
			for (int s = startingIndex, sTimeOffset = vertices + offset; s < maximumSteps + startingIndex; s++, sTimeOffset += vertices) {
				entryExitsLits[i + s] = sTimeOffset + exit;
			}
		}
		solver.addExactly(new VecInt(entryExitsLits), 1);

		/*
			find the shortest path, so weight exits decreasingly and add soft clauses
		 */
		for (int s = 1, sTimeOffset = vertices + offset; s <= maximumSteps; s++, sTimeOffset += vertices) {
			final int finalSTimeOffset = sTimeOffset;
			@NotNull IVecInt exitsLits = VecInt.of(exits.stream().mapToInt(e -> e + finalSTimeOffset).toArray());
			solver.addSoftClause(maximumSteps - s + 1, exitsLits);
		}
	}

	protected boolean addNoValidRouteClause(final @NotNull WeightedPartialMaxsat solver, final int startingVertexOffset, final Agent agent) throws ContradictionException {
		/*
			maximize planned agents so give (|agents| * (max_exit_weight + 1)) soft clause with startingVertex
		 */
		solver.addSoftClause(graph.getEntries() * (maximumSteps + 2), VecInt.of(startingVertexOffset));

		return true;
	}

	private boolean[][] getValidTimeVertices(final int entryID, final @NotNull Set<Integer> exitsID, final long step, final double agentsPerimeter) {
		final int vertices = graph.getVertices().length;
		final boolean[] @NotNull [] validTimeVertices = new boolean[maximumSteps + 1][vertices];
		for (int vertexID = 0; vertexID < vertices; vertexID++) {
			final double startingStep = graph.getDistance(entryID, vertexID);
			final int finalI = vertexID;
			final double finalStep = maximumSteps - exitsID.stream().mapToDouble(exit -> graph.getDistance(finalI, exit)).min().orElse(Double.POSITIVE_INFINITY);
			for (int t = (int) Math.floor(startingStep); t <= finalStep; t++) {
				validTimeVertices[t][vertexID] = !(stepOccupiedVertices.containsKey(step + t) && stepOccupiedVertices.get(step + t).containsKey(vertexID)); // FIXME replace with better check
			}
		}
		return validTimeVertices;
	}

	private void transferAgentPath(final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, final long step, final @NotNull Set<Agent> plannedAgents, final int @NotNull [] model, final @NotNull Triplet<Integer, Agent, boolean[][]> agentOffsetEntry) {
		final int offset = agentOffsetEntry.getVal0();
		final Agent agent = agentOffsetEntry.getVal1();
		final Pair<Integer, Set<Integer>> entryExitsIDs = agentsEntriesExits.get(agent);
		final @NotNull Optional<List<Integer>> pathOptional = createPath(model, offset, entryExitsIDs.getVal0(), entryExitsIDs.getVal1());
		if (pathOptional.isPresent()) {
			final @NotNull List<Integer> path = pathOptional.get();
			assert entryExitsIDs.getVal1().contains(path.get(path.size() - 1));
			agent.setPath(path, step);
			plannedAgents.add(agent);
			addPlannedAgent(agent);
		}
	}

	protected @NotNull Optional<List<Integer>> createPath(int @NotNull [] model, int offset, int entry, @NotNull Set<Integer> exitIDs) {
		final int entryLit = model[offset + entry - 1];
		assert Math.abs(entryLit) - offset == entry;
		if (entryLit < 0) {
			return Optional.empty();
		}

		final @NotNull List<Integer> path = new LinkedList<>();
		path.add(entry);

		final int vertices = graph.getVertices().length;
		for (int s = 1; s <= maximumSteps; s++) {
			final int stepOffset = offset + s * vertices - 1;

			for (int i = 0; i < vertices; i++) {
				final int lit = model[stepOffset + i];
				assert Math.abs(lit) == stepOffset + i + 1;
				if (lit > 0) {
					assert graph.getVertex(path.get(path.size() - 1)).getNeighbourIDs().contains(i) || (allowAgentStop && i == path.get(path.size() - 1));
					path.add(i);
					if (exitIDs.contains(i)) {
						return Optional.of(path);
					}
					break;
				}
			}
		}

		return Optional.of(path);
	}

	@Override
	public @Nullable Agent planAgent(@NotNull Agent agent, final int entryID, final @NotNull Set<Integer> exitsIDs, final long step) {
		final IPBSolver ipbSolver = optimized ? SolverFactory.newDefaultOptimizer() : SolverFactory.newDefault();
		solver = new WeightedMaxSatDecorator(ipbSolver, false);
		final int vertices = graph.getVertices().length;
		solver.newVar(vertices * (maximumSteps + 1));

		try {
			boolean[] @NotNull [] validTimeVertices = getValidTimeVertices(entryID, exitsIDs, step, agent.getAgentPerimeter());

			createAgentClauses(solver, entryID, exitsIDs, new ArrayDeque<>(Collections.singleton(new Triplet<>(1, agent, validTimeVertices))), step, 1);
			if (solver.isSatisfiable()) {
				int[] model = solver.model();
				final @NotNull Optional<List<Integer>> pathOptional = createPath(model, 1, entryID, exitsIDs);
				if (pathOptional.isPresent()) {
					final @NotNull List<Integer> path = pathOptional.get();

					assert exitsIDs.contains(path.get(path.size() - 1));
					agent.setPath(path, step);
				} else {
					return null;
				}
			} else {
				assert false;
				// TODO throw exception
			}
		} catch (ContradictionException | TimeoutException e) {
			e.printStackTrace();
			return null;
		}

		addPlannedAgent(agent);
		return agent;
	}

	@Override
	public void stop() {
		if (solver != null) {
			solver.expireTimeout();
		}
	}
}
