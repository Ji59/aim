package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.*;
import java.util.stream.Collectors;

public class AStar extends SafeLines {
	public AStar(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		double agentsPerimeter = agent.getAgentPerimeter();

		if (stepOccupiedVertices.putIfAbsent(step, new HashMap<>()) != null && !safeVertex(step, agent.getEntry(), agentsPerimeter)) {
			return null;
		}

		Set<Integer> exitIDs;
		if (agent.getExit() > 0) {
			exitIDs = Set.of(agent.getExit());
		} else {
			exitIDs = directionExits.get(agent.getExitDirection()).stream().map(Vertex::getID).collect(Collectors.toSet());
		}

		Map<Integer, Double> heuristic = new HashMap<>();
		int entryID = agent.getEntry();
		heuristic.put(entryID, getHeuristic(exitIDs, entryID));

		PriorityQueue<State> queue = new PriorityQueue<>();
		queue.add(new State(graph.getVertex(entryID), step));

		State state;
		while ((state = queue.poll()) != null) {
			int vertexID = state.getID();
			if (heuristic.get(vertexID) < 0) {
				continue;
			} else {
				heuristic.put(vertexID, -1.);
			}

			if (exitIDs.contains(vertexID)) {
				LinkedList<Integer> path = new LinkedList<>();
				while (state != null) {
					int id = state.getID();
					path.addFirst(id);
					stepOccupiedVertices.get(state.getStep()).put(id, agent);

					state = (State) state.getParent();
				}

				agent.setPath(path, step);
				return agent;
			}

			long stateStep = state.getStep();
			for (int neighbourID : graph.getVertex(vertexID).getNeighbourIDs()) {
				long neighbourStep = stateStep + 1;
				heuristic.putIfAbsent(neighbourID, getHeuristic(exitIDs, neighbourID));

				if (
								(stepOccupiedVertices.putIfAbsent(neighbourStep, new HashMap<>()) == null ||
												(
																safeVertex(neighbourStep, neighbourID, agentsPerimeter) &&
																				safeStepTo(neighbourStep, neighbourID, state.getID(), agentsPerimeter)
												)
								) &&
												safeStepFrom(stateStep, state.getID(), neighbourID, agentsPerimeter)
				) {
					double distance = graph.getDistance(vertexID, neighbourID);
					queue.add(new State(state, graph.getVertex(neighbourID), distance, heuristic.get(neighbourID)));
				}
			}
		}

		return null;
	}

	private double getHeuristic(Set<Integer> exitIDs, int entryID) {
		return exitIDs.stream().mapToDouble(exitID -> graph.getDistance(entryID, exitID)).min().orElse(0);
	}

	private static class State extends SimulationGraph.VertexWithDirection {
		final long step;

		public State(GraphicalVertex vertex, long step) {
			super(vertex);
			this.step = step;
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
