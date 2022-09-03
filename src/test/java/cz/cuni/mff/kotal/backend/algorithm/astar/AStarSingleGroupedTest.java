package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.HexagonalGraph;
import cz.cuni.mff.kotal.simulation.graph.OctagonalGraph;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.junit.jupiter.api.Test;

import java.util.*;

import static cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingleGrouped.*;

class AStarSingleGroupedTest {

	@Test
	void solveAgentsCollisions() {
		Graph graph = new OctagonalGraph(4, 1, 1);
	}

	@Test
	void replanGroupTest() {
		final SimulationGraph graph = new HexagonalGraph(3, 1, 1);

		final List<Agent> agents = List.of(
			new Agent(4, 20, 28, 1, 3, 1.0, 0.0, 0.5800000000000001, 0.31000000000000005, 0.5714285714285714, 0.09056779945936591).setPath(List.of(20, 9, 2, 3, 0, 5, 14, 28), 0),
			new Agent(2, 23, 30, 4, 5, 1.0, 0.0, 0.5800000000000001, 0.31000000000000005, 0.4285714285714286, 0.9094322005406341).setPath(List.of(23, 15, 5, 6, 18, 30), 0),
			new Agent(5, 19, 27, 0, 2, 1.0, 0.0, 0.5800000000000001, 0.31000000000000005, 0.18113559891873182, 0.23342494231650876).setPath(List.of(19, 7, 1, 0, 4, 12, 27), 0),
			new Agent(3, 22, 25, 3, 0, 1.0, 0.0, 0.5800000000000001, 0.31000000000000005, 0.8188644010812682, 0.7665750576834912).setPath(List.of(22, 13, 4, 0, 1, 8, 25), 0),
			new Agent(0, 21, 25, 2, 0, 1.0, 0.0, 0.5800000000000001, 0.31000000000000005, 0.8902929725098396, 0.3571428571428571).setPath(List.of(21, 11, 3, 2, 8, 25), 0),
			new Agent(1, 24, 27, 5, 2, 1.0, 0.0, 0.5800000000000001, 0.31000000000000005, 0.1097070274901604, 0.6428571428571429).setPath(List.of(24, 17, 6, 0, 3, 12, 27), 0)
		);

		final Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups = new HashSet<>(6);
		Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet = null;
		Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet = null;
		final Map<Long, Map<Integer, Set<Agent>>> invalidMovesMap = new HashMap<>();
		final Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable = new HashMap<>();

		for (Agent agent : agents) {
			final Map<Agent, Pair<Integer, Set<Integer>>> agentEntryExits = Map.of(agent, new Pair<>(agent.getEntry(), Set.of(agent.getExit())));
			final Set<Agent> agentCollisionAgents = new HashSet<>();
			final Map<Long, Map<Integer, Agent>> conflictAvoidanceTable = getConflictAvoidanceTable(agentEntryExits, 0);
			final Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> collisionTriplet = new Triplet<>(agentEntryExits, conflictAvoidanceTable, agentCollisionAgents);
			agentsGroups.add(collisionTriplet);
			agent.getAgentPerimeter(graph);

			switch ((int) agent.getId()) {
				case 1 -> {
					agentCollisionAgents.add(agents.get(2));
					agentCollisionAgents.add(agents.get(1));
					agentCollisionAgents.add(agents.get(3));
				}
				case 2, 5 -> {
					mergeConflictAvoidanceTables(invalidMovesMap, conflictAvoidanceTable);
					continue;
				}
				case 3 -> {
					agentCollisionAgents.add(agents.get(2));
					group1CollisionTriplet = collisionTriplet;
					continue;
				}
				case 4 -> {
					agentCollisionAgents.add(agents.get(3));
					group0CollisionTriplet = collisionTriplet;
					continue;
				}
			}

			mergeConflictAvoidanceTables(restGroupsConflictAvoidanceTable, conflictAvoidanceTable);
		}

		final AStarSingleGrouped algorithm = new AStarSingleGrouped(graph, 0, 2, false, Integer.MAX_VALUE, false);

		algorithm.replanGroup(0, agentsGroups, group0CollisionTriplet, group1CollisionTriplet, group0CollisionTriplet.getVal0(), invalidMovesMap, restGroupsConflictAvoidanceTable);

		assert !algorithm.inCollision(agents.get(0), agents.get(1));
		assert !algorithm.inCollision(agents.get(0), agents.get(2));
		assert !algorithm.inCollision(agents.get(0), agents.get(3));
	}
}