package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLines;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
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
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle.*;

public class SATSingleGrouped extends SafeLines {
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SafeLines.PARAMETERS);
	protected static final String MAXIMUM_STEPS_NAME = "Maximum simulation steps";
	protected static final int MAXIMUM_STEPS_DEF = 64;

	static {
		PARAMETERS.put(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		PARAMETERS.put(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);

		PARAMETERS.put(MAXIMUM_STEPS_NAME, MAXIMUM_STEPS_DEF);
	}

	protected final int maximumVertexVisits;
	protected final boolean allowAgentStop;
	protected final int maximumSteps;
	protected final List<Integer>[] inverseNeighbours = new List[graph.getVertices().length];

	public SATSingleGrouped(@NotNull SimulationGraph graph) {
		super(graph);
		maximumSteps = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_STEPS_NAME, MAXIMUM_STEPS_DEF);
		maximumVertexVisits = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		allowAgentStop = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);

		for (int i = 0; i < graph.getVertices().length; i++) {
			inverseNeighbours[i] = new LinkedList<>();
		}
		for (Vertex vertex : graph.getVertices()) {
			for (int neighbourID : vertex.getNeighbourIDs()) {
				inverseNeighbours[neighbourID].add(vertex.getID());
			}
		}
	}

	@Override
	public Collection<Agent> planAgents(@NotNull Collection<Agent> agents, long step) {
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

		filterStepOccupiedVertices(step);

		final WeightedPartialMaxsat solver = new WeightedMaxSatDecorator(org.sat4j.pb.SolverFactory.newDefaultOptimizer(), false);

		final int agentsCount = agentsEntriesExits.size();
		final int vertices = graph.getVertices().length;

		final Deque<Triplet<Integer, Agent, boolean[][]>> offsets = new ArrayDeque<>(agentsCount);

		createAgentsClauses(agentsEntriesExits, step, solver, vertices, offsets);

		final Set<Agent> plannedAgents = new HashSet<>(agentsCount);
		try {
			if (solver.isSatisfiable()) {
				int[] model = solver.model();
				for (Triplet<Integer, Agent, boolean[][]> agentEntry : offsets) {
					transferAgentPath(agentsEntriesExits, step, plannedAgents, model, agentEntry);
				}
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
		}

		return plannedAgents;
	}

	private void createAgentsClauses(final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, final long step, final WeightedPartialMaxsat solver, final int vertices, final Deque<Triplet<Integer, Agent, boolean[][]>> offsets) {
		final int agentsCount = agentsEntriesExits.size();

		solver.newVar(agentsCount * (vertices * (maximumSteps + 1)) + 1);
		int offset = 1;
		for (Map.Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExit : agentsEntriesExits.entrySet()) {
			final Agent agent = agentEntryExit.getKey();
			final int entry = agentEntryExit.getValue().getVal0();
			if (stepOccupiedVertices.get(step).containsKey(entry)) {
				continue;
			}
			Set<Integer> exits = agentEntryExit.getValue().getVal1();

			boolean[][] validTimeVertices = getValidTimeVertices(entry, exits, step, agent.getAgentPerimeter());

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
	 *
	 * @return
	 */
	protected void createAgentClauses(@NotNull WeightedPartialMaxsat solver, int startingVertex, @NotNull Set<Integer> exits, @NotNull Deque<Triplet<Integer, Agent, boolean[][]>> offsets, long step, int agentsCount) throws ContradictionException {
		Triplet<Integer, Agent, boolean[][]> offsetAgent = offsets.getLast();
		final Agent agent = offsetAgent.getVal1();
		final double agentPerimeter = agent.getAgentPerimeter();

		final int vertices = graph.getVertices().length;
		final int offset = offsetAgent.getVal0();

		addAgentsClauses(solver, offsets, step);

		addExitsClause(solver, startingVertex, exits, offset);

		final boolean[][] validTimeVertices = offsetAgent.getVal2();

		/*
			Restrict agent from visiting vertex more than specified
		 */
		final List<Integer>[] validLits = new LinkedList[vertices];
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

			List<Integer> validVerticesIDs = new LinkedList<>();
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
						/*
							∀ vertex v_i different from exit in time t if |V| * t + i + 1 (agent is on v_i in t) then agent has to be on any neighbour in next time
							=> ¬(|V| * t + i + offset) = - (|V| * t + i + offset) ∨ (V_{neighbour j of v_i} |V| * (t + 1) + j + offset)
						*/
						List<Integer> validNeighbours = graph.getVertex(i).getNeighbourIDs().stream()
							.filter(id -> safeStepTo(tStep + 1, id, i, agentPerimeter) && safeStepFrom(tStep, i, id, agentPerimeter))
							.collect(Collectors.toList());
						if (allowAgentStop && i != startingVertex) {
							validNeighbours.add(i);
						}
						if (validNeighbours.isEmpty()) {
							/*
								agent can not be in vertex which he can not get out of
							*/
							solver.addHardClause(VecInt.of(-(timeOffset + i)));
						} else {
							final IVecInt clause = new VecInt(validNeighbours.stream().mapToInt(id -> nextTimeOffset + id).toArray());
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
		for (Triplet<Integer, Agent, boolean[][]> offsetTriplet : offsets) {
			final int neighbourOffset = offsetTriplet.getVal0();
			if (neighbourOffset == agentOffset) {
				continue;
			}

			final double neighbourPerimeter = offsetTriplet.getVal1().getAgentPerimeter();
			final boolean[][] neighbourValidTimeVertices = offsetTriplet.getVal2();
			addAgentsNearVerticesClauses(solver, vertices, agentOffset, agentPerimeter + neighbourPerimeter, validTimeVertices, i, neighbourOffset, neighbourValidTimeVertices);

			for (int neighbourID : graph.getVertex(i).getNeighbourIDs()) {
				for (int predecessorID : inverseNeighbours[i]) {
					addLeavingArrivingClauses(solver, vertices, agentOffset, agentPerimeter, validTimeVertices, i, neighbourOffset, neighbourPerimeter, neighbourValidTimeVertices, neighbourID, predecessorID);
				}
			}
		}
	}

	private void addAgentsNearVerticesClauses(@NotNull WeightedPartialMaxsat solver, int vertices, int agentOffset, double agentsPerimeter, boolean[][] validTimeVertices, int i, int neighbourOffset, boolean[][] neighbourValidTimeVertices) throws ContradictionException {
		for (int conflictVertexID : conflictVertices(i, agentsPerimeter)) {
			for (int t = 0, timeOffset = 0; t <= maximumSteps; t++, timeOffset += vertices) {
				if (validTimeVertices[t][i] && neighbourValidTimeVertices[t][conflictVertexID]) {
					solver.addHardClause(new VecInt(new int[]{-(timeOffset + i + agentOffset), -(timeOffset + conflictVertexID + neighbourOffset)}));
				}
			}
		}
	}

	private void addLeavingArrivingClauses(@NotNull WeightedPartialMaxsat solver, int vertices, int agentOffset, double agentPerimeter, boolean[][] validTimeVertices, int vertexID, int neighbourOffset, double neighbourPerimeter, boolean[][] neighbourValidTimeVertices, int neighbourID, int predecessorID) throws ContradictionException {
    /*
			if agent `a` is going into `v_i` from `v_p` at time `t` and planned agent `n` is going from `v_i` to `v_n` at time `t`, and they would collide,
			agent `a` can not be at `v_p` at `t` or agent `a` can not be at `v_i` at `(t + 1)` or agent `n` can not be at `v_i` at `t` or agent `n` can not be at `v_n` at `(t + 1)`
			=> ¬(|V| * t + p + offset_a) ∨ ¬(|V| * (t + 1) + i + offset_a) ∨ ¬(|V| * t + i + offset_n) ∨ ¬(|V| * (t + 1) + n + offset_n)
		*/
		if (!checkNeighbour(vertexID, predecessorID, neighbourID, agentPerimeter, neighbourPerimeter)) {
			for (int t = 0, timeOffset = 0, nextTimeOffset = vertices; t < maximumSteps; t++, timeOffset += vertices, nextTimeOffset += vertices) {
				if (validTimeVertices[t][predecessorID] && validTimeVertices[t + 1][vertexID] && neighbourValidTimeVertices[t][vertexID] && neighbourValidTimeVertices[t + 1][neighbourID]) {
					solver.addHardClause(VecInt.of(
						-(timeOffset + predecessorID + agentOffset),
						-(nextTimeOffset + vertexID + agentOffset),
						-(timeOffset + vertexID + neighbourOffset),
						-(nextTimeOffset + neighbourID + neighbourOffset)
					));
				}
			}
		}

		/*
			if agent `a` is going into `v_n` from `v_i` at time `t` and planned agent `n` is going from `v_p` to `v_i` at time `t`, and they would collide,
			agent `a` can not be at `v_i` at `t` or agent `a` can not be at `v_n` at `(t + 1)` or agent `n` can not be at `v_p` at `t` or agent `n` can not be at `v_i` at `(t + 1)`
			=> ¬(|V| * t + i + offset_a) ∨ ¬(|V| * (t + 1) + n + offset_a) ∨ ¬(|V| * t + p + offset_n) ∨ ¬(|V| * (t + 1) + i + offset_n)
		*/
		if (!checkNeighbour(vertexID, neighbourID, predecessorID, neighbourPerimeter, agentPerimeter)) {
			for (int t = 0, timeOffset = 0, nextTimeOffset = vertices; t < maximumSteps; t++, timeOffset += vertices, nextTimeOffset += vertices) {
				if (validTimeVertices[t][vertexID] && validTimeVertices[t + 1][neighbourID] && neighbourValidTimeVertices[t][predecessorID] && neighbourValidTimeVertices[t + 1][vertexID]) {
					solver.addHardClause(VecInt.of(
						-(timeOffset + vertexID + agentOffset),
						-(nextTimeOffset + neighbourID + agentOffset),
						-(timeOffset + predecessorID + neighbourOffset),
						-(nextTimeOffset + vertexID + neighbourOffset)
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
	 *
	 * @throws ContradictionException
	 */
	protected void addExitsClause(final @NotNull WeightedPartialMaxsat solver, final int startingVertex, final @NotNull Set<Integer> exits, final int offset) throws ContradictionException {
		final int vertices = graph.getVertices().length;
		int[] entryExitsLits = new int[maximumSteps * exits.size() + 1];
		entryExitsLits[0] = -(startingVertex + offset);

		final Iterator<Integer> exitsIt = exits.iterator();
		for (int i = 0; exitsIt.hasNext(); i += maximumSteps) {
			final int exit = exitsIt.next();
			for (int s = 1, sTimeOffset = vertices + offset; s <= maximumSteps; s++, sTimeOffset += vertices) {
				entryExitsLits[i + s] = sTimeOffset + exit;
			}
		}
		solver.addExactly(new VecInt(entryExitsLits), 1);

		/*
			find the shortest path, so weight exits decreasingly and add soft clauses
		 */
		for (int s = 1, sTimeOffset = vertices + offset; s <= maximumSteps; s++, sTimeOffset += vertices) {
			final int finalSTimeOffset = sTimeOffset;
			IVecInt exitsLits = VecInt.of(exits.stream().mapToInt(e -> e + finalSTimeOffset).toArray());
			solver.addSoftClause(maximumSteps - s + 1, exitsLits);
		}

		/*
			maximize planned agents so give (|agents| * (max_exit_weight + 1)) soft clause with startingVertex
		 */
		solver.addSoftClause(graph.getEntries() * (maximumSteps + 2), VecInt.of(startingVertex + offset));
	}

	private boolean[][] getValidTimeVertices(final int entryID, final @NotNull Set<Integer> exitsID, final long step, final double agentsPerimeter) {
		final int vertices = graph.getVertices().length;
		final boolean[][] validTimeVertices = new boolean[maximumSteps + 1][vertices];
		for (int vertexID = 0; vertexID < vertices; vertexID++) {
			final double startingStep = graph.getDistance(entryID, vertexID);
			final int finalI = vertexID;
			final double finalStep = maximumSteps - exitsID.stream().mapToDouble(exit -> graph.getDistance(finalI, exit)).min().orElse(Double.POSITIVE_INFINITY);
			for (int t = (int) Math.floor(startingStep); t < finalStep; t++) {
				validTimeVertices[t][vertexID] = safeVertex(step + t, vertexID, agentsPerimeter);
			}
		}
		return validTimeVertices;
	}

	private void transferAgentPath(final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, final long step, final Set<Agent> plannedAgents, final int[] model, final Triplet<Integer, Agent, boolean[][]> agentOffsetEntry) {
		final int offset = agentOffsetEntry.getVal0();
		final Agent agent = agentOffsetEntry.getVal1();
		final Pair<Integer, Set<Integer>> entryExitsIDs = agentsEntriesExits.get(agent);
		final Optional<List<Integer>> pathOptional = createPath(model, offset, entryExitsIDs.getVal0(), entryExitsIDs.getVal1());
		if (pathOptional.isPresent()) {
			final List<Integer> path = pathOptional.get();
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

		final List<Integer> path = new LinkedList<>();
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
		WeightedPartialMaxsat solver = new WeightedMaxSatDecorator(org.sat4j.pb.SolverFactory.newDefaultOptimizer(), false);
		final int vertices = graph.getVertices().length;
		solver.newVar(vertices * (maximumSteps + 1));

		try {
			boolean[][] validTimeVertices = getValidTimeVertices(entryID, exitsIDs, step, agent.getAgentPerimeter());

			createAgentClauses(solver, entryID, exitsIDs, new ArrayDeque<>(Collections.singleton(new Triplet<>(1, agent, validTimeVertices))), step, 1);
			if (solver.isSatisfiable()) {
				int[] model = solver.model();
				final Optional<List<Integer>> pathOptional = createPath(model, 1, entryID, exitsIDs);
				if (pathOptional.isPresent()) {
					final List<Integer> path = pathOptional.get();

					assert exitsIDs.contains(path.get(path.size() - 1));
					agent.setPath(path, step);
				}
			} else {
				return null;
			}
		} catch (ContradictionException | TimeoutException e) {
			e.printStackTrace();
			return null;
		}

		addPlannedAgent(agent);
		return agent;
	}
}
