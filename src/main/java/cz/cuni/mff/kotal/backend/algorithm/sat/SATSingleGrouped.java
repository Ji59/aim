package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle;
import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLines;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.maxsat.WeightedPartialMaxsat;
import org.sat4j.pb.IPBSolver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle.*;

public class SATSingleGrouped extends SafeLines {
	protected static final String MAXIMUM_STEPS_NAME = "Maximum simulation steps";
	protected static final int MAXIMUM_STEPS_DEF = 64;

	@Deprecated
	protected static final String ALTERNATIVE_ALGORITHM_NAME = "Alternative algorithm";
	@Deprecated
	protected static final String ALTERNATIVE_ALGORITHM_DEF = ""; // AlgorithmMenuTab2.Parameters.Algorithm.A_STAR.getName(); // TODO
	protected static final String ALLOW_INCOMPLETE_PLANS_NAME = "Allow incomplete plans";
	protected static final boolean ALLOW_INCOMPLETE_PLANS_DEF = false;
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SafeLines.PARAMETERS);

	static {
		PARAMETERS.put(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		PARAMETERS.put(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);

		PARAMETERS.put(MAXIMUM_STEPS_NAME, MAXIMUM_STEPS_DEF);
		PARAMETERS.put(ALLOW_INCOMPLETE_PLANS_NAME, ALLOW_INCOMPLETE_PLANS_DEF);
		PARAMETERS.put(ALTERNATIVE_ALGORITHM_NAME, ALTERNATIVE_ALGORITHM_DEF); // TODO remove
	}

	protected final int maximumVertexVisits;
	protected final boolean allowAgentStop;
	protected final int maximumSteps;
	@Deprecated
	protected final Algorithm alternativeAlgorithm;
	protected final boolean allowIncompletePlans;
	protected final List<Integer>[] inverseNeighbours = new List[graph.getVertices().length];

	@TestOnly
	private final AStar aStar;

	public SATSingleGrouped(SimulationGraph graph) {
		super(graph);
		maximumSteps = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_STEPS_NAME, MAXIMUM_STEPS_DEF);
		final String alternativeAlgorithmName = AlgorithmMenuTab2.getStringParameter(ALTERNATIVE_ALGORITHM_NAME, ALTERNATIVE_ALGORITHM_DEF);
		AlgorithmMenuTab2.Parameters.Algorithm algorithmEnum = AlgorithmMenuTab2.Parameters.Algorithm.nameOf(alternativeAlgorithmName);
		Algorithm algorithm;
		try {
			if (algorithmEnum == null) {
				algorithm = (agent, entryID, exitsID, step) -> null;
			} else {
				algorithm = algorithmEnum.getAlgorithmClass().getConstructor(SimulationGraph.class).newInstance(graph);
			}
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ex) {
			algorithm = (agent, entryID, exitsID, step) -> null;
		}
		alternativeAlgorithm = algorithm;

		maximumVertexVisits = AlgorithmMenuTab2.getIntegerParameter(MAXIMUM_VERTEX_VISITS_NAME, MAXIMUM_VERTEX_VISITS_DEF);
		allowAgentStop = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
		allowIncompletePlans = AlgorithmMenuTab2.getBooleanParameter(ALLOW_INCOMPLETE_PLANS_NAME, ALLOW_INCOMPLETE_PLANS_DEF);

		for (int i = 0; i < graph.getVertices().length; i++) {
			inverseNeighbours[i] = new LinkedList<>();
		}
		for (Vertex vertex : graph.getVertices()) {
			for (int neighbourID : vertex.getNeighbourIDs()) {
				inverseNeighbours[neighbourID].add(vertex.getID());
			}
		}

		aStar = new AStar(graph);
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
	protected void createAgentClauses(WeightedPartialMaxsat solver, int startingVertex, Set<Integer> exits, Deque<Triplet<Integer, Agent, boolean[][]>> offsets, long step, int agentsCount) throws ContradictionException {
		Triplet<Integer, Agent, boolean[][]> offsetAgent = offsets.getLast();
		final Agent agent = offsetAgent.getVal1();
		final double agentPerimeter = agent.getAgentPerimeter();

		final int vertices = graph.getVertices().length;
		final int offset = offsetAgent.getVal0();

		addAgentsClauses(solver, offsets, step);

		// In time 0: agent is at entry v_en => (en + 1)
//		unitClauses.push(startingVertex + offset);

		/*
			agent can be at one exit over all steps
			so exactly one is true from ∪_{i>=1} { offset + vertices * i + e | e ∈ exits }
			if agent cannot be planned, (offset + startingVertex) is false
		 */
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
			solver.addSoftClause(2 * maximumSteps - 2 * s + 1, exitsLits);
		}

		/*
			maximize planned agents so give (|agents| * max_exit_weight) soft clause with startingVertex
		 */
		solver.addSoftClause(agentsCount * (maximumSteps + 2), VecInt.of(startingVertex + offset));

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

				/*
					if agent is at exit, it cannot be at any other exit and next step it has to be nowhere
					that is represented by constrain: at most one is true in { offset + e | e ∈ exits } ∪ { offset + |V| + v | v ∈ V }
				 */
//				final int[] exitsLits = new int[vertices + exits.size()];
//				int vertex = 0;
//				for (; vertex < vertices; vertex++) {
//					exitsLits[vertex] = nextTimeOffset + vertex;
//				}
//
//				for (final int exit : exits) {
//					exitsLits[vertex++] = timeOffset + exit;
//				}
//
//				solver.addAtMost(new VecInt(exitsLits), 1);

				for (int i : validVerticesIDs) {
					if (exits.contains(i)) {
						/*
							for exit v_e if agent is at v_e in time t, then in all next times he is nowhere
							=> (|V| * t + e + offset) -> ∀_{time s > t} (∧_{vertices j:} ¬(|V| * s + j + offset))
							<=> ∀_{time s > t} ∀_{j in vertices}: -(|V| * t + e + offset) ∨ -(|V| * s + j + offset)
						*/
//						for (int s = t + 1, sTimeOffset = nextTimeOffset; s <= maximumSteps; s++, sTimeOffset += vertices) {
//							for (int j = 0; j < vertices; j++) {
//								agentConstrains.add(solver.addClause(new VecInt(new int[]{-(timeOffset + i), -(sTimeOffset + j)})));
//							}
//						}
					} else {
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

//						FIXME
//						if (!allowMultipleVisits) {
//							/*
//								if agent is at `v_i` at time `t`, agent can not ve at `v_i` at time `s != t`
//								=> (|V| * t + i + offset) -> (∀_{s != t} ¬(|V| * s + i + offset))
//								<=> ∀_{s != t} (¬(|V| * t + i + offset) ∨ ¬(|V| * s + i + offset))
//								<=> ∀_{s > t} (-(|V| * t + i + offset) ∨ -(|V| * s + i + offset))
//							*/
//							for (int s = t + 1, sTimeOffset = timeOffset + vertices; s <= maximumSteps; s++, sTimeOffset += vertices) {
//								agentConstrains.add(solver.addClause(new VecInt(new int[]{-(timeOffset + i), -(sTimeOffset + i)})));
//							}
//						}
					}
				}
			}
		}

		if (!allowIncompletePlans) {
			/*
				agent has to end in any exit vertex in any step
				=> ∨_{time t, exit e} (|V| * t + e + offset)
			*/
			int[] exitClause = new int[maximumSteps * exits.size()];
			int i = 0;
			for (Integer exit : exits) {
				for (int t = 0; t < maximumSteps; t++) {
					exitClause[t + i * maximumSteps] = vertices * (t + 1) + exit + offset;
				}
				i++;
			}
			solver.addHardClause(new VecInt(exitClause));
		}
	}

	protected void addAgentsClauses(WeightedPartialMaxsat solver, Deque<Triplet<Integer, Agent, boolean[][]>> offsets, long step) throws ContradictionException {
		final int vertices = graph.getVertices().length;
		Triplet<Integer, Agent, boolean[][]> last = offsets.getLast();
		final int agentOffset = last.getVal0();
		final double agentPerimeter = last.getVal1().getAgentPerimeter();
		final boolean[][] validTimeVertices = last.getVal2();

		addPlanedAgentsClauses(solver, step, vertices, agentOffset, agentPerimeter, validTimeVertices);

		for (int i = 0; i < vertices; i++) {
			addAgentsVertexClauses(solver, offsets, vertices, agentOffset, agentPerimeter, validTimeVertices, i);
		}

	}

	private void addPlanedAgentsClauses(WeightedPartialMaxsat solver, long step, int vertices, int agentOffset, double agentPerimeter, boolean[][] validTimeVertices) throws ContradictionException {
		for (int t = 0; t <= maximumSteps; t++) {
			final int timeOffset = t * vertices;
			long tStep = step + t;
			if (!stepOccupiedVertices.containsKey(tStep)) {
				continue;
			}
			for (int i = 0; i < vertices; i++) {
				if (validTimeVertices[t][i]) {
					final int vertexTimeOffset = timeOffset + i;
					if (!safeVertex(tStep, i, agentPerimeter)) {
					/*
						Agent can not be near occupied vertex
					*/
						solver.addHardClause(VecInt.of(-(vertexTimeOffset + agentOffset)));
					}
				}
			}
		}
	}

	private void addAgentsVertexClauses(WeightedPartialMaxsat solver, Deque<Triplet<Integer, Agent, boolean[][]>> offsets, int vertices, int agentOffset, double agentPerimeter, boolean[][] validTimeVertices, int i) throws ContradictionException {
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

	private void addAgentsNearVerticesClauses(WeightedPartialMaxsat solver, int vertices, int agentOffset, double agentsPerimeter, boolean[][] validTimeVertices, int i, int neighbourOffset, boolean[][] neighbourValidTimeVertices) throws ContradictionException {
		for (int conflictVertexID : conflictVertices(i, agentsPerimeter)) {
			for (int t = 0, timeOffset = 0; t <= maximumSteps; t++, timeOffset += vertices) {
				if (validTimeVertices[t][i] && neighbourValidTimeVertices[t][conflictVertexID]) {
					solver.addHardClause(new VecInt(new int[]{-(timeOffset + i + agentOffset), -(timeOffset + conflictVertexID + neighbourOffset)}));
				}
			}
		}
	}

	private void addLeavingArrivingClauses(WeightedPartialMaxsat solver, int vertices, int agentOffset, double agentPerimeter, boolean[][] validTimeVertices, int i, int neighbourOffset, double neighbourPerimeter, boolean[][] neighbourValidTimeVertices, int neighbourID, int predecessorID) throws ContradictionException {
    /*
			if agent `a` is going into `v_i` from `v_p` at time `t` and planned agent `n` is going from `v_i` to `v_n` at time `t`, and they would collide,
			agent `a` can not be at `v_p` at `t` or agent `a` can not be at `v_i` at `(t + 1)` or agent `n` can not be at `v_i` at `t` or agent `n` can not be at `v_n` at `(t + 1)`
			=> ¬(|V| * t + p + offset_a) ∨ ¬(|V| * (t + 1) + i + offset_a) ∨ ¬(|V| * t + i + offset_n) ∨ ¬(|V| * (t + 1) + n + offset_n)
		*/
		if (!checkNeighbour(i, predecessorID, neighbourID, agentPerimeter, neighbourPerimeter)) {
			for (int t = 0, timeOffset = 0, nextTimeOffset = vertices; t < maximumSteps; t++, timeOffset += vertices, nextTimeOffset += vertices) {
				if (validTimeVertices[t][predecessorID] && validTimeVertices[t + 1][i] && neighbourValidTimeVertices[t][i] && neighbourValidTimeVertices[t + 1][neighbourID]) {
					solver.addHardClause(VecInt.of(
						-(timeOffset + predecessorID + agentOffset),
						-(nextTimeOffset + i + agentOffset),
						-(timeOffset + i + neighbourOffset),
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
		if (!checkNeighbour(i, neighbourID, predecessorID, neighbourPerimeter, agentPerimeter)) {
			for (int t = 0, timeOffset = 0, nextTimeOffset = vertices; t < maximumSteps; t++, timeOffset += vertices, nextTimeOffset += vertices) {
				if (validTimeVertices[t][i] && validTimeVertices[t + 1][neighbourID] && neighbourValidTimeVertices[t][predecessorID] && neighbourValidTimeVertices[t + 1][i]) {
					solver.addHardClause(VecInt.of(
						-(timeOffset + i + agentOffset),
						-(nextTimeOffset + neighbourID + agentOffset),
						-(timeOffset + predecessorID + neighbourOffset),
						-(nextTimeOffset + i + neighbourOffset)
					));
				}
			}
		}
	}

	protected Optional<List<Integer>> createPath(int[] model, int offset, int entry, Set<Integer> exitIDs) {
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

//		for (int var : model) {
//			if (var > 0 && var >= offset && var < offset + vertices * (maximumSteps + 1)) {
//				path.add((var - offset) % vertices);
//			}
//		}

		return Optional.of(path);
	}

	protected Agent createFinalPath(Agent agent, Set<Integer> exits, long step, List<Integer> path) {
		if (path.size() == 0 || !Objects.equals(path.get(0), agent.getEntry())) throw new AssertionError();
		int lastVertex = path.get(path.size() - 1);
		if (!exits.contains(lastVertex)) {
			Agent agent1 = null;
			try {
				agent1 = alternativeAlgorithm.planAgent(agent, lastVertex, exits, step + path.size() - 1);
				if (agent1 != null) {
					combinePaths(agent1, path, step);
				}
			} finally {
				// FIXME replace exception in algorithms with null
				if (agent1 == null) {
					agent = null;
				}
			}
		} else {
			agent.setPath(path, step);
		}
		return agent;
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
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
	public Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		System.out.println("Computing " + step);
		if (agentsEntriesExits.isEmpty()) {
			return agentsEntriesExits.keySet();
		}
		final WeightedPartialMaxsat solver = new WeightedMaxSatDecorator(org.sat4j.pb.SolverFactory.newDefaultOptimizer(), false);
//		solver.setTimeoutMs(500);  // TODO

		final int agentsCount = agentsEntriesExits.size();
		final int vertices = graph.getVertices().length;
		solver.newVar(agentsCount * (vertices * (maximumSteps + 1)) + 1);

		stepOccupiedVertices.keySet().removeIf(stepKey -> stepKey < step);
		stepOccupiedVertices.putIfAbsent(step, new HashMap<>());
		Map<Agent, Pair<Integer, Set<Integer>>> invalidAgents = new HashMap<>();

		Deque<Triplet<Integer, Agent, boolean[][]>> offsets = new ArrayDeque<>(agentsCount);
		int offset = 1;
		for (Map.Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExit : agentsEntriesExits.entrySet()) {
			Agent agent = agentEntryExit.getKey();
			final int entry = agentEntryExit.getValue().getVal0();
			if (stepOccupiedVertices.get(step).containsKey(entry)) {
				continue;
			}
			Set<Integer> exits = agentEntryExit.getValue().getVal1();

			boolean[][] validTimeVertices = getValidTimeVertices(entry, exits, vertices);

			offsets.add(new Triplet<>(offset, agent, validTimeVertices));
			try {
				createAgentClauses(solver, entry, exits, offsets, step, agentsCount);
				offset += vertices * (maximumSteps + 1);
			} catch (ContradictionException e) {
				System.out.println(e.getMessage());
				invalidAgents.put(agent, agentEntryExit.getValue());
				offsets.removeLast();
			}
		}

		final Set<Agent> plannedAgents = new HashSet<>(agentsCount);
		Map<Agent, List<Integer>> partiallyPlannedAgents = new HashMap<>();
		try {
			if (solver.isSatisfiable()) {
				int[] model = solver.model();
				for (Triplet<Integer, Agent, boolean[][]> entry : offsets) {
					offset = entry.getVal0();
					final Agent agent = entry.getVal1();
					final Pair<Integer, Set<Integer>> entryExitsIDs = agentsEntriesExits.get(agent);
					final Optional<List<Integer>> pathOptional = createPath(model, offset, entryExitsIDs.getVal0(), entryExitsIDs.getVal1());
					if (pathOptional.isEmpty()) {
						invalidAgents.put(agent, entryExitsIDs);
						continue;
					}
					final List<Integer> path = pathOptional.get();
					Set<Integer> exits = entryExitsIDs.getVal1();
					int lastVertex = path.get(path.size() - 1);
					if (exits.contains(lastVertex)) {
						agent.setPath(path, step);
						plannedAgents.add(agent);
						addPlannedAgent(agent);
						alternativeAlgorithm.addPlannedAgent(agent);
					} else {
						partiallyPlannedAgents.put(agent, path);
						invalidAgents.put(agent, new Pair<>(lastVertex, exits));
					}
				}
			} else {
				plannedAgents.addAll(alternativeAlgorithm.planAgents(agentsEntriesExits, step));
				for (Agent agent : plannedAgents) {
					addPlannedAgent(agent);
				}
			}
		} catch (TimeoutException e) {
			System.out.println(e.getMessage());
			plannedAgents.addAll(alternativeAlgorithm.planAgents(agentsEntriesExits, step));
			for (Agent agent : plannedAgents) {
				addPlannedAgent(agent);
			}
		}

		Collection<Agent> invalidPlannedAgents = alternativeAlgorithm.planAgents(invalidAgents, step);
		for (
			Agent agent : invalidPlannedAgents) {
			if (partiallyPlannedAgents.containsKey(agent)) {
				List<Integer> path = partiallyPlannedAgents.get(agent);
				combinePaths(agent, path, step);
				alternativeAlgorithm.addPlannedPath(agent, path, step);
			}
			addPlannedAgent(agent);
		}

		plannedAgents.addAll(invalidPlannedAgents);
		return plannedAgents;
	}

	protected void combinePaths(Agent agent, List<Integer> path, long step) {
		path.remove(path.size() - 1);
		path.addAll(agent.getPath());
		agent.setPath(path, step);
	}

	@Override
	public Agent planAgent(Agent agent, int entryID, Set<Integer> exitsIDs, long step) {
		WeightedPartialMaxsat solver = new WeightedMaxSatDecorator(org.sat4j.pb.SolverFactory.newDefaultOptimizer(), false);
		final int vertices = graph.getVertices().length;
		solver.newVar(vertices * (maximumSteps + 1));

		try {
			boolean[][] validTimeVertices = getValidTimeVertices(entryID, exitsIDs, vertices);

			createAgentClauses(solver, entryID, exitsIDs, new ArrayDeque<>(Collections.singleton(new Triplet<>(1, agent, validTimeVertices))), step, 1);
			if (solver.isSatisfiable()) {
				int[] model = solver.model();
				final Optional<List<Integer>> pathOptional = createPath(model, 1, entryID, exitsIDs);
				if (pathOptional.isPresent()) {
					final BigInteger violatedWeight = solver.violatedWeight();
					final List<Integer> path = pathOptional.get();
					final List<Integer> aStarPath = aStar.getPath(agent, step, entryID, exitsIDs);
					if (aStarPath != null && aStarPath.size() < path.size()) {
						int[] aStarPathLits = new int[aStarPath.size()];
						for (int i = 0; i < aStarPath.size(); i++) {
							int id = aStarPath.get(i);
							aStarPathLits[i] = id + vertices * i + 1;
						}
						assert solver.isSatisfiable(VecInt.of(aStarPathLits));
						model = solver.model();
						BigInteger violatedWeight1 = solver.violatedWeight();
						System.out.println(violatedWeight + "; " + violatedWeight1);
						assert false;
					}

					agent = createFinalPath(agent, exitsIDs, step, path);
					aStar.addPlannedAgent(agent);
				} else {
					agent = alternativeAlgorithm.planAgent(agent, step);
				}
			} else {
				agent = alternativeAlgorithm.planAgent(agent, step);
			}
		} catch (ContradictionException | TimeoutException e) {
			agent = alternativeAlgorithm.planAgent(agent, step);
		}

		if (agent != null) {
			addPlannedAgent(agent);
			alternativeAlgorithm.addPlannedAgent(agent);
		}

		return agent;
	}

	private boolean[][] getValidTimeVertices(int entryID, Set<Integer> exitsID, int vertices) {
		final boolean[][] validTimeVertices = new boolean[maximumSteps + 1][vertices];
		for (int i = 0; i < vertices; i++) {
			final double startingStep = graph.getDistance(entryID, i);
			final int finalI = i;
			final double finalStep = maximumSteps - exitsID.stream().mapToDouble(exit -> graph.getDistance(finalI, exit)).min().orElse(Double.POSITIVE_INFINITY);
			for (int t = (int) Math.floor(startingStep); t < finalStep; t++) {
				validTimeVertices[t][i] = true;
			}
		}
		return validTimeVertices;
	}

	private static class AStar extends AStarSingle {
		public AStar(SimulationGraph graph) {
			super(graph);
		}

		public @Nullable LinkedList<Integer> getPath(Agent agent, long step, int entryID, Set<Integer> exitsIDs) {
			return super.getPath(agent, step, entryID, exitsIDs);
		}
	}
}

class Scratch {
	public static void main(String[] args) throws ContradictionException, TimeoutException {
		final int NBCLAUSES = 4;

		final IPBSolver pbsolver = org.sat4j.maxsat.SolverFactory.newDefault();
		WeightedPartialMaxsat solver = new WeightedMaxSatDecorator(org.sat4j.pb.SolverFactory.newDefaultOptimizer(), false);

		// prepare the solver to accept MAXVAR variables. MANDATORY for MAXSAT solving
		solver.newVar(6);
//		solver.setExpectedNumberOfClauses(NBCLAUSES);
		solver.setTimeout(69);

		IVecInt exactlyLits = new VecInt(new int[]{1, 2, 3, 4, 5, 6});

		solver.addExactly(exactlyLits, 1);

		IVecInt clause1 = new VecInt(new int[]{1});
		IVecInt clause2 = new VecInt(new int[]{2});
		IVecInt clause3 = new VecInt(new int[]{3});
		IVecInt clause4 = new VecInt(new int[]{4});
		IVecInt clause5 = new VecInt(new int[]{5});
		IVecInt clause6 = new VecInt(new int[]{6});

		solver.addSoftClause(6, clause1);
		solver.addSoftClause(5, clause2);
		solver.addSoftClause(4, clause3);
		solver.addSoftClause(3, clause4);
		solver.addSoftClause(2, clause5);
		solver.addSoftClause(1, clause6);


// we are done. Working now on the IProblem interface
		if (solver.isSatisfiable()) {
			System.out.println(Arrays.toString(solver.model()));
			System.out.println(solver.violatedWeight());
		} else {
			System.out.println("Unsatisfiable");
		}
	}
}
