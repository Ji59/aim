package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirection;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AStarSingleGrouped extends AStarSingle {

	public static final Map<String, Object> PARAMETERS = AStarSingle.PARAMETERS;

	public AStarSingleGrouped(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		stepOccupiedVertices.keySet().removeIf(stepKey -> stepKey < step);
		return planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), getExitIDs(a)))), step);
	}

	@Override
	public Collection<Agent> planAgents(final Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		stepOccupiedVertices.putIfAbsent(step, new HashMap<>());

		Iterator<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator();

		// filter agents that cannot enter intersection
		while (it.hasNext()) {
			Map.Entry<Agent, Pair<Integer, Set<Integer>>> entry = it.next();
			Agent agent = entry.getKey();
			int agentEntry = entry.getValue().getVal0();
			if (!safeVertex(step, agentEntry, agent.getAgentPerimeter())) {
				it.remove();
			}
		}

		if (agentsEntriesExits.isEmpty()) {
			return Collections.emptySet();
		}

		Collection<Agent> agents = agentsEntriesExits.keySet();

		for (int i = agentsEntriesExits.size(); i > 0; i--) {
			for (Collection<Agent> agentsCombination: MyNumberOperations.combinations(agents, i)) {
				final Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExitsSubset = new HashMap<>(i);
				for (Agent agent : agentsCombination) {
					agentsEntriesExitsSubset.put(agent, agentsEntriesExits.get(agent));
				}
				Collection<Agent> plannedAgents = findPaths(agentsEntriesExitsSubset, step);
				if (!plannedAgents.isEmpty()) {
					return plannedAgents;
				}
			}

		}
		return Collections.emptySet();
	}

	protected Collection<Agent> findPaths(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		final int agentsCount = agentsEntriesExits.size();
		final Set<Integer>[] exitsIDs = new Set[agentsCount];
		final Agent[] agents = new Agent[agentsCount];
		final double[][] heuristics = new double[agentsCount][];
		final int[] maximumDelays = new int[agentsCount];

		final Set<CompositeState> visitedStates = new HashSet<>();
		final Map<Integer, Set<Integer>>[][] collisionPairs = new Map[agentsCount][];
		final Map<Integer, Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>>>[][] collisionTransfers = new Map[agentsCount][];
		for (int i = 0; i < agentsCount; i++) {
			collisionPairs[i] = new Map[i];
			collisionTransfers[i] = new Map[i];
			for (int j = 0; j < i; j++) {
				collisionPairs[i][j] = new HashMap<>();
				collisionTransfers[i][j] = new HashMap<>();
			}
		}

		final CompositeState initialState = generateInitialState(agentsEntriesExits, exitsIDs, agents, heuristics, maximumDelays, step);
		final PriorityQueue<CompositeState> queue = new PriorityQueue<>();
		queue.add(initialState);

		queue:
		while (!queue.isEmpty()) {
			final CompositeState state = queue.poll();
			if (state.isFinal(exitsIDs)) {
				final List<Integer>[] paths = constructPaths(state);
				for (int i = 0; i < agentsCount; i++) {
					Agent agent = agents[i];
					agent.setPath(paths[i], step);
					addPlannedAgent(agent);
				}
				return List.of(agents);
			}

			if (visitedStates.contains(state)) {
				continue;
			} else {
				visitedStates.add(state);
			}

			long stateStep = state.getStep();
			final long nextStep = stateStep + 1;
			stepOccupiedVertices.putIfAbsent(nextStep, new HashMap<>());

			// check if all non-finished agents can continue their journey
			for (int i = 0; i < agentsCount; i++) {
				if (nextStep - step > maximumDelays[i] && !exitsIDs[i].contains(state.getStates()[i].getID())) {
					continue queue;
				}
			}

			final Set<Integer>[] neighbours = new Set[agentsCount];
			final Map<Integer, Integer>[] neighboursVisited = new Map[agentsCount];
			for (int i = 0; i < agentsCount; i++) {
				Set<Integer> vertexNeighbours = new HashSet<>(state.getStates()[i].getVertex().getNeighbourIDs());
				neighbours[i] = vertexNeighbours;
				if (allowAgentStop) {
					vertexNeighbours.add(state.getStates()[i].getID());
				}

				if (!allowAgentReturn && state.getParent() != null) {
					vertexNeighbours.remove(state.getParent().getStates()[i].getID());
				}

				neighboursVisited[i] = new HashMap<>(neighbours[i].size());
				for (Iterator<Integer> iterator = vertexNeighbours.iterator(); iterator.hasNext(); ) {
					final int id = iterator.next();
					if (heuristics[i][id] == 0) {
						heuristics[i][id] = getHeuristic(exitsIDs[i], id);
					}
					if (Double.isFinite(heuristics[i][id])) {
						neighboursVisited[i].put(id, 1);
					} else {
						iterator.remove();
					}
				}
			}

			// Remove neighbours visited too many times
			CompositeState predecessorState = state;
			while (predecessorState != null) {
				for (int i = 0; i < agentsCount; i++) {
					int predecessorVertexID = predecessorState.getStates()[i].getID();
					if (neighbours[i].contains(predecessorVertexID)) {
						int vertexVisits = neighboursVisited[i].get(predecessorVertexID);
						if (vertexVisits >= maximumVertexVisits) {
							neighbours[i].remove(predecessorVertexID);
						} else {
							neighboursVisited[i].put(predecessorVertexID, vertexVisits + 1);
						}
					}
				}
				predecessorState = predecessorState.getParent();
			}

			// remove colliding neighbours
			for (int i = 0; i < agentsCount; i++) {
				final int stateID = state.getStates()[i].getID();
				for (Iterator<Integer> iterator = neighbours[i].iterator(); iterator.hasNext(); ) {
					final int neighbourID = iterator.next();
					final double agentPerimeter = agents[i].getAgentPerimeter();
					if (!safeVertex(nextStep, neighbourID, agentPerimeter) ||
						!safeStepTo(nextStep, neighbourID, stateID, agentPerimeter) ||
						!safeStepFrom(stateStep, stateID, neighbourID, agentPerimeter)
					) {
						iterator.remove();
					}
				}
			}

			// if any traveling agent does not have any valid neighbours, skip generating neighbours states
			for (int i = 0; i < agentsCount; i++) {
				if (neighbours[i].isEmpty() && !exitsIDs[i].contains(state.getStates()[i].getID())) {
					continue queue;
				}
			}

			// for each valid neighbour create vertex object
			final VertexWithDirection[][] neighboursVertices = new VertexWithDirection[agentsCount][];
			for (int i = 0; i < agentsCount; i++) {
				final VertexWithDirection stateVertex = state.getStates()[i];
				final Set<Integer> vertexNeighbours = neighbours[i];
				if (vertexNeighbours.isEmpty()) {
					neighboursVertices[i] = new VertexWithDirection[]{stateVertex};
				} else {
					VertexWithDirection[] neighboursWithVisit = new VertexWithDirection[vertexNeighbours.size()];
					neighboursVertices[i] = neighboursWithVisit;
					int j = 0;
					for (int id : vertexNeighbours) {
						final double distance = graph.getDistance(stateVertex.getID(), id);
						neighboursWithVisit[j++] = new VertexWithDirection(stateVertex, graph.getVertex(id), distance, heuristics[i][id]);
					}
				}
			}

			// add cartesian product of agents position vertex neighbours to queue
			Set<VertexWithDirection[]> cartesianNeighbours = cartesianProductWithCheck(agents, exitsIDs, collisionPairs, collisionTransfers, state, neighboursVertices);
			neighbours:
			for (VertexWithDirection[] neighboursCombination : cartesianNeighbours) {
				CompositeState nextState = new CompositeState(neighboursCombination, state);
				if (visitedStates.contains(nextState)) {
					continue;
				}
				queue.add(nextState);
			}
		}

		return Collections.emptySet();
	}

	private Set<VertexWithDirection[]> cartesianProductWithCheck(final Agent[] agents, Set<Integer>[] exitsIDs, final Map<Integer, Set<Integer>>[][] collisionPairs, final Map<Integer, Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>>>[][] collisionTransfers, final CompositeState previousState, final VertexWithDirection[][] neighbours) {
		return cartesianProductWithCheck(agents, exitsIDs, collisionPairs, collisionTransfers, previousState, neighbours, new VertexWithDirection[agents.length], 0);
	}

	private Set<VertexWithDirection[]> cartesianProductWithCheck(final Agent[] agents, final Set<Integer>[] exitsIDs, final Map<Integer, Set<Integer>>[][] collisionPairs, final Map<Integer, Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>>>[][] collisionTransfers, final CompositeState previousState, final VertexWithDirection[][] neighbours, final VertexWithDirection[] constructed, final int addingIndex) {
		Set<VertexWithDirection[]> cartesian = new HashSet<>();
		final int nextIndex = addingIndex + 1;

		for (VertexWithDirection vertex : neighbours[addingIndex]) {
			int vertexID = vertex.getID();
			if (!exitsIDs[addingIndex].contains(vertexID) && collisionVertexPair(agents, collisionPairs, collisionTransfers, previousState, constructed, vertexID, addingIndex)) {
				continue;
			}

			VertexWithDirection[] clonedArray = constructed.clone();
			clonedArray[addingIndex] = vertex;
			if (nextIndex >= agents.length) {
				cartesian.add(clonedArray);
			} else {
				cartesian.addAll(cartesianProductWithCheck(agents, exitsIDs, collisionPairs, collisionTransfers, previousState, neighbours, clonedArray, nextIndex));
			}
		}

		return cartesian;
	}

	private boolean collisionVertexPair(final Agent[] agents, final Map<Integer, Set<Integer>>[][] collisionPairs, final Map<Integer, Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>>>[][] collisionTransfers, final CompositeState previousState, final VertexWithDirection[] neighboursCombination, final int vertex, int agentIndex) {
		final double agentPerimeter = agents[agentIndex].getAgentPerimeter();
		for (int j = 0; j < agentIndex; j++) {
			final int vertex1 = neighboursCombination[j].getID();
			final double neighbourPerimeter = agents[j].getAgentPerimeter();
			Map<Integer, Set<Integer>> collisionPairIJ = collisionPairs[agentIndex][j];
			if (!collisionPairIJ.containsKey(vertex)) {
				Set<Integer> vertex0CollisionVertices = conflictVerticesSet(vertex, agentPerimeter + neighbourPerimeter);
				collisionPairIJ.put(vertex, vertex0CollisionVertices);
			}

			if (collisionPairIJ.get(vertex).contains(vertex1)) {
				return true;
			}

			if (vertex == previousState.getStates()[j].getID()) {
				collisionTransfers[agentIndex][j].computeIfAbsent(vertex, k -> new HashMap<>());

				Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>> vertex0CollisionPairs = collisionTransfers[agentIndex][j].get(vertex);
				int predecessorI = previousState.getStates()[agentIndex].getID();
				Pair<Integer, Integer> predINextJPair = new Pair<>(predecessorI, vertex1);
				vertex0CollisionPairs.computeIfAbsent(predINextJPair, k -> new Pair<>(null, null));
				Pair<Boolean, Boolean> transferCollision = vertex0CollisionPairs.get(predINextJPair);
				if (transferCollision.getVal0() == null) {
					transferCollision.setVal0(!checkNeighbour(vertex, predecessorI, vertex1, agentPerimeter, neighbourPerimeter));
				}

				if (transferCollision.getVal0()) {
					return true;
				}
			}

			if (vertex1 == previousState.getStates()[agentIndex].getID()) {
				collisionTransfers[agentIndex][j].computeIfAbsent(vertex, k -> new HashMap<>());

				Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>> vertex0CollisionPairs = collisionTransfers[agentIndex][j].get(vertex);
				int predecessorJ = previousState.getStates()[j].getID();
				Pair<Integer, Integer> nextIPredJPair = new Pair<>(vertex, predecessorJ);
				vertex0CollisionPairs.computeIfAbsent(nextIPredJPair, k -> new Pair<>(null, null));
				Pair<Boolean, Boolean> transferCollision = vertex0CollisionPairs.get(nextIPredJPair);
				if (transferCollision.getVal1() == null) {
					transferCollision.setVal1(!checkNeighbour(vertex1, predecessorJ, vertex, neighbourPerimeter, agentPerimeter));
				}

				if (transferCollision.getVal1()) {
					return true;
				}
			}
		}
		return false;
	}

	private List<Integer>[] constructPaths(CompositeState state) {
		final int agents = state.getStates().length;
		final LinkedList<Integer>[] paths = new LinkedList[agents];
		for (int i = 0; i < agents; i++) {
			paths[i] = new LinkedList<>();
			paths[i].add(state.getStates()[i].getID());
		}
		final boolean[] notExitVertices = new boolean[agents];

		while (state != null) {
			for (int i = 0; i < agents; i++) {
				if (notExitVertices[i] || paths[i].get(0) != state.getStates()[i].getID()) {
					paths[i].addFirst(state.getStates()[i].getID());
					notExitVertices[i] = true;
				}
			}
			state = state.getParent();
		}

		return paths;
	}

	private CompositeState generateInitialState(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, Set<Integer>[] exitsIDs, Agent[] agents, double[][] heuristics, int[] maximumDelays, long step) {
		final VertexWithDirection[] initialStates = new VertexWithDirection[agents.length];
		final Iterator<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator();
		for (int i = 0; it.hasNext(); i++) {
			final Map.Entry<Agent, Pair<Integer, Set<Integer>>> agentPairEntry = it.next();
			final Agent agent = agentPairEntry.getKey();
			agents[i] = agent;

			final Set<Integer> exits = agentPairEntry.getValue().getVal1();
			exitsIDs[i] = exits;

			final int entryID = agentPairEntry.getValue().getVal0();
			final double startEstimate = getHeuristic(exits, entryID);
			final VertexWithDirection initialState = new VertexWithDirection(graph.getVertex(entryID), 0, startEstimate);
			initialStates[i] = initialState;

			heuristics[i] = new double[graph.getVertices().length];
			heuristics[i][entryID] = startEstimate;

			maximumDelays[i] = exits.stream().mapToInt(exitID -> (int) (Math.ceil(graph.getDistance(entryID, exitID)) + super.maximumPathDelay)).min().getAsInt();
		}

		return new CompositeState(initialStates, step);
	}

	protected static class CompositeState implements Comparable<CompositeState> {
		private final VertexWithDirection[] states;
		private final long step;

		private final CompositeState parent;

		protected CompositeState(VertexWithDirection[] states, long step) {
			this.states = states;
			this.step = step;
			this.parent = null;
		}

		protected CompositeState(VertexWithDirection[] states, CompositeState parent) {
			this.states = states;
			this.step = parent.step + 1;
			this.parent = parent;
		}

		protected double getEstimate() {
			double estimate = 0;
			for (VertexWithDirection vertex : states) {
				if (vertex != null) {
					estimate += vertex.getEstimate() + vertex.getDistance();
				}
			}

			return estimate;
		}

		protected boolean isFinal(Set<Integer>[] exitsIDs) {
			for (int i = 0; i < states.length; i++) {
				if (!exitsIDs[i].contains(states[i].getID())) {
					return false;
				}
			}

			return true;
		}

		public VertexWithDirection[] getStates() {
			return states;
		}

		public long getStep() {
			return step;
		}

		public CompositeState getParent() {
			return parent;
		}

		@Override
		public int compareTo(@NotNull CompositeState collectiveState) {
			return Double.compare(this.getEstimate(), collectiveState.getEstimate());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CompositeState state = (CompositeState) o;

			if (step != state.step) return false;
			// Probably incorrect - comparing Object[] arrays with Arrays.equals
			return Arrays.equals(states, state.states);
		}

		@Override
		public int hashCode() {
			int result = Arrays.hashCode(states);
			result = 31 * result + (int) (step ^ (step >>> 32));
			return result;
		}
	}
}
