package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.sat4j.core.ConstrGroup;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SATPlanner extends SafeLines {
	protected static final String MAXIMUM_STEPS_NAME = "Maximum simulation steps";
	protected static final int MAXIMUM_STEPS_DEF = 64;
	protected static final String ALTERNATIVE_ALGORITHM_NAME = "Alternative algorithm";
	protected static final String ALTERNATIVE_ALGORITHM_DEF = AlgorithmMenuTab2.Parameters.Algorithm.A_STAR.getName();
	protected static final String ALLOW_INCOMPLETE_PLANS_NAME = "Allow incomplete plans";
	protected static final boolean ALLOW_INCOMPLETE_PLANS_DEF = false;
	protected static final String ALLOW_AGENT_STOP_NAME = "Allow agent stop";
	protected static final boolean ALLOW_AGENT_STOP_DEF = false;
	protected static final String ALLOW_MULTIPLE_VISITS_NAME = "Allow visit visited vertex";
	protected static final boolean ALLOW_MULTIPLE_VISITS_DEF = false;
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SafeLines.PARAMETERS);

	static {
		PARAMETERS.put(MAXIMUM_STEPS_NAME, MAXIMUM_STEPS_DEF);
		PARAMETERS.put(ALTERNATIVE_ALGORITHM_NAME, ALTERNATIVE_ALGORITHM_DEF);
		PARAMETERS.put(ALLOW_INCOMPLETE_PLANS_NAME, ALLOW_INCOMPLETE_PLANS_DEF);
		PARAMETERS.put(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
		PARAMETERS.put(ALLOW_MULTIPLE_VISITS_NAME, ALLOW_MULTIPLE_VISITS_DEF);
	}

	private final int maximumSteps;
	private final Algorithm alternativeAlgorithm;
	private final boolean allowIncompletePlans;
	private final boolean allowAgentStop;
	private final boolean allowMultipleVisits;
	protected final List<Integer>[] inverseNeighbours = new List[graph.getVertices().length];
	protected final Map<Integer, Set<Integer>> directionExits = new HashMap<>();

	public SATPlanner(SimulationGraph graph) {
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

		allowIncompletePlans = AlgorithmMenuTab2.getBooleanParameter(ALLOW_INCOMPLETE_PLANS_NAME, ALLOW_INCOMPLETE_PLANS_DEF);
		allowAgentStop = AlgorithmMenuTab2.getBooleanParameter(ALLOW_AGENT_STOP_NAME, ALLOW_AGENT_STOP_DEF);
		allowMultipleVisits = AlgorithmMenuTab2.getBooleanParameter(ALLOW_MULTIPLE_VISITS_NAME, ALLOW_AGENT_STOP_DEF);

		for (int i = 0; i < graph.getVertices().length; i++) {
			inverseNeighbours[i] = new LinkedList<>();
		}
		for (Vertex vertex : graph.getVertices()) {
			for (int neighbourID : vertex.getNeighbourIDs()) {
				inverseNeighbours[neighbourID].add(vertex.getID());
			}
		}

		graph.getEntryExitVertices().forEach((key, value) ->
			directionExits.put(
				key,
				value.stream()
					.filter(vertex -> vertex.getType().isExit())
					.map(Vertex::getID)
					.collect(Collectors.toSet())
			)
		);
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
	 * @return
	 */
	protected Pair<IVecInt, ConstrGroup> createAgentClauses(ISolver solver, int startingVertex, Set<Integer> exits, Deque<Pair<Integer, Agent>> offsets, long step) throws ContradictionException {
		Pair<Integer, Agent> offsetAgent = offsets.getLast();
		Agent agent = offsetAgent.getVal1();
		double agentPerimeter = agent.getAgentPerimeter();

		Pair<IVecInt, ConstrGroup> unitAndConstrains = addAgentsClauses(solver, offsets, step);
		IVecInt unitClauses = unitAndConstrains.getVal0();
		ConstrGroup agentConstrains = unitAndConstrains.getVal1();

		final int vertices = graph.getVertices().length;
		final int offset = offsetAgent.getVal0();

		// In time 0: agent is at entry v_en => (en + 1)
		unitClauses.push(startingVertex + offset);

		for (int t = 0; t <= maximumSteps; t++) {
			final int timeOffset = vertices * t + offset;

			/*
				if agent is at v_i in time t, he is not in v_j != v_i
				=> (|V| * t + i + offset) -> (∧_{vertex v_j, i != j} ¬(|V| * t + j + ))
				<=> ∀_{vertex j != i} - (|V| * t + i + offset) ∨ - (|V| * t + j + offset)
				<=> ∀_{vertex j < i} - (|V| * t + i + offset) ∨ - (|V| * t + j + offset)
			*/
			for (int i = 1; i < vertices; i++) {
				for (int j = 0; j < i; j++) {
					agentConstrains.add(solver.addClause(new VecInt(new int[]{-(timeOffset + i), -(timeOffset + j)})));
				}
			}

			if (t < maximumSteps) {
				/*
					Relations between time t and (t + 1)
				*/
				final long tStep = step + t;
				final int nextTimeOffset = timeOffset + vertices;
				for (int i = 0; i < vertices; i++) {
					if (exits.contains(i)) {
						/*
							for exit v_e if agent is at v_e in time t, then in all next times he is nowhere
							=> (|V| * t + e + offset) -> ∀_{time s > t} (∧_{vertices j:} ¬(|V| * s + j + offset))
							<=> ∀_{time s > t} ∀_{j in vertices}: -(|V| * t + e + offset) ∨ -(|V| * s + j + offset)
						*/
						for (int s = t + 1, sTimeOffset = nextTimeOffset; s <= maximumSteps; s++, sTimeOffset += vertices) {
							for (int j = 0; j < vertices; j++) {
								agentConstrains.add(solver.addClause(new VecInt(new int[]{-(timeOffset + i), -(sTimeOffset + j)})));
							}
						}
					} else {
						/*
							∀ vertex v_i different from exit in time t if |V| * t + i + 1 (agent is on v_i in t) then agent has to be on any neighbour in next time
							=> ¬(|V| * t + i + offset) = - (|V| * t + i + offset) ∨ (V_{neighbour j of v_i} |V| * (t + 1) + j + offset)
						*/
						int finalI = i;
						List<Integer> validNeighbours = graph.getVertex(i).getNeighbourIDs().stream()
							.filter(id -> safeStepTo(tStep + 1, id, finalI, agentPerimeter) && safeStepFrom(tStep, finalI, id, agentPerimeter))
							.collect(Collectors.toList());
						if (allowAgentStop && graph.getVertex(i).getType().equals(Vertex.Type.ROAD)) {
							validNeighbours.add(i);
						}
						if (validNeighbours.size() == 0) {
							/*
								agent can not be in vertex which he can not get out of
							*/
							unitClauses.push(-(timeOffset + i));
						} else {
							VecInt clause = new VecInt(validNeighbours.stream().mapToInt(id -> nextTimeOffset + id).toArray());
							clause.push(-(timeOffset + i));
							// TODO allow agent to stay in place
							agentConstrains.add(solver.addClause(clause));


						}

						if (!allowMultipleVisits) {
							/*
								if agent is at `v_i` at time `t`, agent can not ve at `v_i` at time `s != t`
								=> (|V| * t + i + offset) -> (∀_{s != t} ¬(|V| * s + i + offset))
								<=> ∀_{s != t} (¬(|V| * t + i + offset) ∨ ¬(|V| * s + i + offset))
								<=> ∀_{s > t} (-(|V| * t + i + offset) ∨ -(|V| * s + i + offset))
							*/
							for (int s = t + 1, sTimeOffset = timeOffset + vertices; s <= maximumSteps; s++, sTimeOffset += vertices) {
								agentConstrains.add(solver.addClause(new VecInt(new int[]{-(timeOffset + i), -(sTimeOffset + i)})));
							}
						}
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
			agentConstrains.add(solver.addClause(new VecInt(exitClause)));
		}

		return unitAndConstrains;
	}

	protected Pair<IVecInt, ConstrGroup> addAgentsClauses(ISolver solver, Deque<Pair<Integer, Agent>> offsets, long step) throws ContradictionException {
		final int vertices = graph.getVertices().length;
		final int agentOffset = offsets.getLast().getVal0();
		final double agentPerimeter = offsets.getLast().getVal1().getAgentPerimeter();

		IVecInt unitClauses = new VecInt();
		ConstrGroup agentConstrains = new ConstrGroup();

		for (int t = 0; t <= maximumSteps; t++) {
			final int timeOffset = t * vertices;
			long tStep = step + t;
			if (!stepOccupiedVertices.containsKey(tStep)) {
				continue;
			}
			for (int i = 0; i < vertices; i++) {
				final int vertexTimeOffset = timeOffset + i;
				if (!safeVertex(tStep, i, agentPerimeter)) {
					/*
						Agent can not be near occupied vertex
					*/
					unitClauses.push(-(vertexTimeOffset + agentOffset));
				}
			}
		}

		for (int i = 0; i < vertices; i++) {
					/*
						if agent `a_i` is at vertex `v_x` at time `t`, no other agent `n` can be at near vertex `v_y` at time `t`.
						=> ∀_{vertex x, time t} ((|V| * t + x + offset_i) -> ∧_{agent j != i, vertex y near x} ¬(|V| * t + y + offset_j))
						<=> ∀_{vertex x, time t} ∀_{agent j != i} ∀_{vertex y near x} (¬(|V| * t + x + offset_i) ∨ ¬(|V| * t + y + offset_j))
					*/
			for (Pair<Integer, Agent> offsetPair : offsets) {
				final Integer neighbourOffset = offsetPair.getVal0();
				final Agent neighbour = offsetPair.getVal1();
				if (neighbourOffset == agentOffset) {
					continue;
				}
				for (int conflictVertexID : conflictVertices(i, agentPerimeter + neighbour.getAgentPerimeter())) {
					for (int t = 0, timeOffset = 0; t <= maximumSteps; t++, timeOffset += vertices) {
						agentConstrains.add(solver.addClause(new VecInt(new int[]{-(timeOffset + i + agentOffset), -(timeOffset + conflictVertexID + neighbourOffset)})));
					}
				}

				for (int neighbourID : graph.getVertex(i).getNeighbourIDs()) {
					for (int predecessorID : inverseNeighbours[i]) {
						/*
							if agent `a` is going into `v_i` from `v_p` at time `t` and planned agent `n` is going from `v_i` to `v_n` at time `t`, and they would collide,
							agent `a` can not be at `v_p` at `t` or agent `a` can not be at `v_i` at `(t + 1)` or agent `n` can not be at `v_i` at `t` or agent `n` can not be at `v_n` at `(t + 1)`
							=> ¬(|V| * t + p + offset_a) ∨ ¬(|V| * (t + 1) + i + offset_a) ∨ ¬(|V| * t + i + offset_n) ∨ ¬(|V| * (t + 1) + n + offset_n)
						*/
						if (!checkNeighbour(i, predecessorID, neighbourID, agentPerimeter, neighbour)) {
							for (int t = 0, timeOffset = 0, nextTimeOffset = vertices; t < maximumSteps; t++, timeOffset += vertices, nextTimeOffset += vertices) {
								agentConstrains.add(solver.addClause(new VecInt(new int[]{-(timeOffset + predecessorID + agentOffset), -(nextTimeOffset + i + agentOffset), -(timeOffset + i + neighbourOffset), -(nextTimeOffset + neighbourID + neighbourOffset)})));
							}
						}

						/*
							if agent `a` is going into `v_n` from `v_i` at time `t` and planned agent `n` is going from `v_p` to `v_i` at time `t`, and they would collide,
							agent `a` can not be at `v_i` at `t` or agent `a` can not be at `v_n` at `(t + 1)` or agent `n` can not be at `v_p` at `t` or agent `n` can not be at `v_i` at `(t + 1)`
							=> ¬(|V| * t + i + offset_a) ∨ ¬(|V| * (t + 1) + n + offset_a) ∨ ¬(|V| * t + p + offset_n) ∨ ¬(|V| * (t + 1) + i + offset_n)
						*/
						if (!checkNeighbour(i, neighbourID, predecessorID, agentPerimeter, neighbour)) {
							for (int t = 0, timeOffset = 0, nextTimeOffset = vertices; t < maximumSteps; t++, timeOffset += vertices, nextTimeOffset += vertices) {
								agentConstrains.add(solver.addClause(new VecInt(new int[]{-(timeOffset + i + agentOffset), -(timeOffset + vertices + neighbourID + agentOffset), -(timeOffset + predecessorID + neighbourOffset), -(nextTimeOffset + i + neighbourOffset)})));
							}
						}
					}
				}
			}
		}

		return new Pair<>(unitClauses, agentConstrains);
	}

	protected List<Integer> createPath(int[] model, int offset) {
		LinkedList<Integer> path = new LinkedList<>();

		for (int var : model) {
			if (var > 0 && var >= offset && var < offset + graph.getVertices().length * (maximumSteps + 1)) {
				path.add((var - offset) % graph.getVertices().length);
			}
		}

		return path;
	}

	protected Agent createFinalPath(Agent agent, Set<Integer> exits, long step, List<Integer> path) {
		if (path.size() == 0 || !Objects.equals(path.get(0), agent.getEntry())) throw new AssertionError();
		int lastVertex = path.get(path.size() - 1);
		if (exits.contains(lastVertex)) {
			Agent agent1 = null;
			try {
				agent1 = alternativeAlgorithm.planAgent(agent, lastVertex, exits, step + path.size() - 1);
				if (agent1 != null) {
					path.remove(path.size() - 1);
					path.addAll(agent1.getPath());
					agent.setPath(path, step);
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
						a.getExit() < 0 ? directionExits.get(a.getExitDirection()) : Collections.singleton(a.getExit())
					)
				)),
			step
		);
	}

	@Override
	public Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		if (agentsEntriesExits.isEmpty()) {
			return agentsEntriesExits.keySet();
		}
		ISolver solver = SolverFactory.newDefault();
		solver.setTimeoutMs(500);  // TODO

		stepOccupiedVertices.keySet().removeIf(stepKey -> stepKey < step);
		stepOccupiedVertices.putIfAbsent(step, new HashMap<>());
		Map<Agent, Pair<Integer, Set<Integer>>> invalidAgents = new HashMap<>();

		Deque<Pair<Integer, Agent>> offsets = new ArrayDeque<>(agentsEntriesExits.size());
		int offset = 1;
		IVecInt validUnitClauses = new VecInt();
		for (Map.Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExit : agentsEntriesExits.entrySet()) {
			Agent agent = agentEntryExit.getKey();
			int entry = agentEntryExit.getValue().getVal0();
			if (stepOccupiedVertices.get(step).containsKey(entry)) {
				continue;
			}
			Set<Integer> exits = agentEntryExit.getValue().getVal1();

			offsets.add(new Pair<>(offset, agent));
			Pair<IVecInt, ConstrGroup> unitAndConstrains = null;
			try {
				unitAndConstrains = createAgentClauses(solver, entry, exits, offsets, step);
				IVecInt unitClauses = unitAndConstrains.getVal0();
				if (solver.isSatisfiable(unitClauses)) {
					unitClauses.copyTo(validUnitClauses);
					offset += graph.getVertices().length * (maximumSteps + 1);
				} else {
					unitAndConstrains.getVal1().removeFrom(solver);
					invalidAgents.put(agent, agentsEntriesExits.get(agent));
					offsets.removeLast();
				}
			} catch (ContradictionException | TimeoutException e) {
				System.out.println(e.getMessage());
				if (unitAndConstrains != null) {
					unitAndConstrains.getVal1().removeFrom(solver);
				}
				invalidAgents.put(agent, agentEntryExit.getValue());
				offsets.removeLast();
			}
		}

		Set<Agent> plannedAgents = new HashSet<>(agentsEntriesExits.size());
		Map<Agent, List<Integer>> partiallyPlannedAgents = new HashMap<>();
		try {
			if (solver.isSatisfiable(validUnitClauses)) {
				int[] model = solver.model();
				for (Pair<Integer, Agent> entry : offsets) {
					offset = entry.getVal0();
					Agent agent = entry.getVal1();
					List<Integer> path = createPath(model, offset);
					if (path.size() == 0 || !Objects.equals(path.get(0), agentsEntriesExits.get(agent).getVal0())) throw new AssertionError();  // TODO remove
					Set<Integer> exits = agentsEntriesExits.get(agent).getVal1();
					int lastVertex = path.get(path.size() - 1);
					if (exits.contains(lastVertex)) {
						agent.setPath(path, step);
						plannedAgents.add(agent);
					} else {
						partiallyPlannedAgents.put(agent, path);
						invalidAgents.put(agent, new Pair<>(lastVertex, exits));
					}
				}
			} else {
				plannedAgents.addAll(alternativeAlgorithm.planAgents(agentsEntriesExits, step));
			}
		} catch (TimeoutException e) {
			System.out.println(e.getMessage());
			plannedAgents.addAll(alternativeAlgorithm.planAgents(agentsEntriesExits, step));
		}

		for (Agent agent : plannedAgents) {
			addPlannedAgent(agent);
		}

		Collection<Agent> invalidPlannedAgents = alternativeAlgorithm.planAgents(invalidAgents, step);
		for (Agent agent : invalidPlannedAgents) {
			if (partiallyPlannedAgents.containsKey(agent)) {
				List<Integer> path = partiallyPlannedAgents.get(agent);
				path.remove(path.size() - 1);
				path.addAll(agent.getPath());
				agent.setPath(path, step);
			}
			addPlannedAgent(agent);
		}

		plannedAgents.addAll(invalidPlannedAgents);
		return plannedAgents;
	}

	@Override
	public void addPlannedAgent(Agent agent) {
		List<Integer> path = agent.getPath();
		long step = agent.getPlannedTime();
		for (int i = 0; i < path.size(); i++) {
			stepOccupiedVertices.putIfAbsent(step + i, new HashMap<>());
			stepOccupiedVertices.get(step + i).put(path.get(i), agent);
			alternativeAlgorithm.addPlannedAgent(agent);
		}
	}

	@Override
	public Agent planAgent(Agent agent, int entryID, Set<Integer> exitsID, long step) {
		ISolver solver = SolverFactory.newDefault();
		solver.setTimeout(1);  // TODO

		try {
			Pair<IVecInt, ConstrGroup> unitAndConstrains = createAgentClauses(solver, entryID, exitsID, new ArrayDeque<>(Collections.singleton(new Pair<>(1, agent))), step);
			IVecInt unitClauses = unitAndConstrains.getVal0();
			if (solver.isSatisfiable(unitClauses)) {
				int[] model = solver.model();
				List<Integer> path = createPath(model, 1);
				agent = createFinalPath(agent, exitsID, step, path);
			}
		} catch (ContradictionException | TimeoutException e) {
			agent = alternativeAlgorithm.planAgent(agent, step);
		}

		if (agent != null) {
			addPlannedAgent(agent);
		}

		return agent;
	}
}

class Scratch {
	public static void main(String[] args) throws ContradictionException, TimeoutException {
		final int MAXVAR = 16;
		final int NBCLAUSES = 4;

		ISolver solver = SolverFactory.newDefault();

		// prepare the solver to accept MAXVAR variables. MANDATORY for MAXSAT solving
//		solver.newVar(MAXVAR);
//		solver.setExpectedNumberOfClauses(NBCLAUSES);
		solver.setTimeout(69);
		// Feed the solver using Dimacs format, using arrays of int
		// (best option to avoid dependencies on SAT4J IVecInt)
//		for (int i = 1; i <= NBCLAUSES / 2; i++) {
		// get the clause from somewhere
		// the clause should not contain a 0, only integer (positive or negative)
		// with absolute values less or equal to MAXVAR
		// e.g. int [] clause = {1, -3, 7}; is fine
		// while int [] clause = {1, -3, 7, 0}; is not fine
		IVecInt clause0 = new VecInt(new int[]{1, 2, -3});
		IVecInt clause1 = new VecInt(new int[]{-1});
		IVecInt clause2 = new VecInt(new int[]{-2});
		IVecInt clause3 = new VecInt(new int[]{1, 3});

		ConstrGroup c = new ConstrGroup();
		c.add(solver.addClause(clause1));
		solver.addClause(clause0);
		solver.addClause(clause2);
		solver.addClause(clause3);


		Vec<IVecInt> clauses = new Vec<>();
		clauses.push(clause1);
		clauses.push(clause2);
		clauses.push(clause3);
		clauses.push(clause0);
//		solver.addAllClauses(clauses);
//		}

// we are done. Working now on the IProblem interface
		if (solver.isSatisfiable()) {
			System.out.println(Arrays.toString(solver.model()));
		} else {
			System.out.println("Unsatisfiable");
		}

		c.removeFrom(solver);

		if (solver.isSatisfiable()) {
			System.out.println(Arrays.toString(solver.model()));
		} else {
			System.out.println("Unsatisfiable");
		}
	}
}
