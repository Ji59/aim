package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.backend.algorithm.LinkGraph;
import cz.cuni.mff.kotal.backend.algorithm.LinkVertex;
import cz.cuni.mff.kotal.backend.algorithm.simple.Roundabout;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.*;

public class AStarRoundabout extends AStarSingle {
	protected static final String LANES_NAME = "Lanes";
	protected static final Integer LANES_DEF = 2;

	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(AStarSingle.PARAMETERS);

	static {
		PARAMETERS.put(LANES_NAME, LANES_DEF);
	}

	private final int lanes;

	public AStarRoundabout(@NotNull SimulationGraph graph) {
		this(graph, true);
	}

	protected AStarRoundabout(@NotNull SimulationGraph graph, boolean oriented) {
		super(createRoundaboutGraph(graph, AlgorithmMenuTab2.getIntegerParameter(LANES_NAME, LANES_DEF), oriented));
		lanes = AlgorithmMenuTab2.getIntegerParameter(LANES_NAME, LANES_DEF);
	}

	@Override
	public Agent planAgent(@NotNull Agent agent, long step) {
		LinkGraph linkGraph = (LinkGraph) graph;
		int entryID = linkGraph.getLinkID(agent.getEntry());

		@Nullable List<Integer> path = getPath(agent, step, entryID, getExitIDs(agent));

		if (path == null) {
			return null;
		}

		addPlannedAgent(agent.setPath(path, step));
		return agent;
	}

	@Override
	@NotNull
	protected Set<Integer> getExitIDs(@NotNull Agent agent) {
		if (agent.getExit() >= 0) {
			return Set.of(((LinkGraph) graph).getLinkID(agent.getExit()));
		} else {
			return directionExits.get(agent.getExitDirection());
		}
	}

	public static @NotNull LinkGraph createRoundaboutGraph(@NotNull SimulationGraph baseGraph, int lanes, boolean oriented) {
		@NotNull LinkGraph graph = new LinkGraph(oriented, baseGraph);
		int id = 0;

		@NotNull List<LinkVertex> vertices = new ArrayList<>();
		for (Map.@NotNull Entry<Integer, List<Vertex>> entry : baseGraph.getEntryExitVertices().entrySet()) {
			for (Vertex vertex : entry.getValue()) {
				@NotNull LinkVertex linkVertex = new LinkVertex(id++, (GraphicalVertex) vertex);
				vertices.add(linkVertex);
				graph.addVertexMapping(linkVertex);
			}
		}

		@NotNull List<Integer> loop = createInitialLoop(baseGraph, oriented, graph, id, vertices);

		for (int lane = 1; lane < lanes && !loop.isEmpty(); lane++) {
			id += loop.size();

			@NotNull List<? extends Vertex> newLoop = getNewLoopBaseVertices(baseGraph, graph, id, loop, vertices);

			if (newLoop.size() == 1) {
				Vertex vertex = newLoop.get(0);
				LinkVertex linkVertex = vertices.get(graph.getLinkID(vertex.getID()));

				for (int neighbourID : vertex.getNeighbourIDs()) {
					addNonDirectNeighbours(baseGraph, graph, vertices, linkVertex, neighbourID);
				}

				break;
			}

			@NotNull List<Integer> newLoopIndexes = newLoop.stream().map(Vertex::getID).toList();

			addNewLoopNeighbours(oriented, baseGraph, graph, vertices, newLoop, newLoopIndexes);

			addOldNewLoopsNeighbours(oriented, baseGraph, graph, vertices, loop, newLoop);

			loop = newLoopIndexes;
		}

		graph.setVertices(vertices);

		// add entries neighbours
		graph.getEntryExitVertices().values().stream()
			.flatMap(List::stream)
			.filter(v -> v.getType().isEntry())
			.forEach(v -> {
				@NotNull List<Integer> neighbours = v.getNeighbourIDs().stream().map(graph::getLinkID).toList();
				LinkVertex linkVertex = graph.getLinkVertexByReal(v.getID());
				for (int neighbourID : neighbours) {
					LinkVertex neighbour = vertices.get(neighbourID);
					addNeighbour(linkVertex, neighbour, baseGraph, graph);
				}
			});

		return graph;
	}

	private static void addNonDirectNeighbours(@NotNull Graph baseGraph, @NotNull LinkGraph graph, @NotNull List<LinkVertex> vertices, @NotNull LinkVertex linkVertex, int neighbourID) {
		LinkVertex neighbour = vertices.get(graph.getLinkID(neighbourID));
		addNeighbour(linkVertex, neighbour, baseGraph, graph);
		addNeighbour(neighbour, linkVertex, baseGraph, graph);
	}

	private static void addNeighbour(@NotNull LinkVertex linkVertex, @NotNull LinkVertex neighbour, @NotNull Graph baseGraph, @NotNull LinkGraph graph) {
		linkVertex.addNeighbourID(neighbour.getID());
		graph.getEdges().add(new Edge(linkVertex, neighbour, baseGraph.getDistance(linkVertex.getRealID(), neighbour.getRealID())));
	}

	private static void addOldNewLoopsNeighbours(boolean oriented, @NotNull Graph baseGraph, @NotNull LinkGraph graph, @NotNull List<LinkVertex> vertices, @NotNull List<Integer> loop, @NotNull List<? extends Vertex> newLoop) {
		for (@NotNull Vertex vertex : newLoop) {
			LinkVertex linkVertex = vertices.get(graph.getLinkID(vertex.getID()));
			@NotNull List<Integer> oldLoopNeighbours = new LinkedList<>();
			for (int oldLoopID : loop) {
				if (vertex.getNeighbourIDs().contains(oldLoopID)) {
					oldLoopNeighbours.add(oldLoopID);
				}
			}

			if (oriented) {
				int firstIndex = 0;
				int maxDistanceToNext = -1;
				for (int i = 0; i < oldLoopNeighbours.size(); i++) {
					int nextIndex = (i + 1) % oldLoopNeighbours.size();
					int distanceToNext = getLaneDistance(loop, oldLoopNeighbours.get(i), oldLoopNeighbours.get(nextIndex));
					if (distanceToNext > maxDistanceToNext) {
						firstIndex = nextIndex;
						maxDistanceToNext = distanceToNext;
						if (maxDistanceToNext > loop.size() / 2) {
							break;
						}
					}
				}

				for (int i = 0; i < Math.ceil(oldLoopNeighbours.size() / 2.); i++) {
					int firstID = oldLoopNeighbours.get((i + firstIndex) % oldLoopNeighbours.size());
					LinkVertex neighbour = vertices.get(graph.getLinkID(firstID));
					addNeighbour(neighbour, linkVertex, baseGraph, graph);

					int lastID = oldLoopNeighbours.get(myModulo(firstIndex - i - 1, oldLoopNeighbours.size()));
					neighbour = vertices.get(graph.getLinkID(lastID));
					addNeighbour(linkVertex, neighbour, baseGraph, graph);
				}
			} else {
				for (int vertexID : oldLoopNeighbours) {
					addNonDirectNeighbours(baseGraph, graph, vertices, linkVertex, vertexID);
				}
			}
		}
	}

	private static @NotNull List<? extends Vertex> getNewLoopBaseVertices(@NotNull SimulationGraph baseGraph, @NotNull LinkGraph graph, int id, @NotNull List<Integer> loop, @NotNull List<LinkVertex> vertices) {
		@NotNull List<GraphicalVertex> newLoop = new LinkedList<>();

		int lastNewLoopID;
		int oldLoopFirstIndex = 0;

		do {
			Vertex firstOld = baseGraph.getVertex(loop.get(oldLoopFirstIndex));
			lastNewLoopID = firstOld.getNeighbourIDs().stream()
				.filter(neighbour -> graph.getLinkID(neighbour) == null)
				.min((id0, id1) -> {
					double distance0 = baseGraph.getDistance(id0, loop.get(loop.size() - 1));
					double distance1 = baseGraph.getDistance(id1, loop.get(loop.size() - 1));
					int distancesComparison = Double.compare(distance0, distance1);
					if (distancesComparison != 0) {
						return distancesComparison;
					}
					int neighbours0 = baseGraph.getVertex(id0).getNeighbourIDs().size();
					int neighbours1 = baseGraph.getVertex(id1).getNeighbourIDs().size();
					return neighbours1 - neighbours0;
				})
				.orElse(-1);
		} while (lastNewLoopID < 0 && ++oldLoopFirstIndex < loop.size());
		if (lastNewLoopID < 0) {
			return newLoop;
		}

		lastLoopCycle:
		for (int i = 0; i < loop.size(); i++) {
			int index = (i + oldLoopFirstIndex) % loop.size();
			Integer vertexID = loop.get(index);
			GraphicalVertex lastOldLoopVertex = baseGraph.getVertex(vertexID);

			int newLoopID = lastNewLoopID;
			while (newLoopID >= 0) {
				GraphicalVertex lastNewLoopVertex = baseGraph.getVertex(newLoopID);

				if (!graph.existsMapping(newLoopID)) {
					lastNewLoopID = newLoopID;
					newLoop.add(lastNewLoopVertex);

					@NotNull LinkVertex linkVertex = new LinkVertex(id++, lastNewLoopVertex);
					vertices.add(linkVertex);
					graph.addVertexMapping(linkVertex);
				}
				for (int j = newLoop.size() - 1; lastNewLoopVertex.getNeighbourIDs().stream().allMatch(graph::existsMapping); j--) {
					if (j <= 0) {
						break lastLoopCycle;
					} else {
						lastNewLoopVertex = newLoop.get(j);
					}
				}

				newLoopID = getNeighboursIntersection(graph, lastOldLoopVertex, lastNewLoopVertex, loop.get(myModulo(index - 1, loop.size())));
			}
		}

		return newLoop;
	}

	private static int getNeighboursIntersection(@NotNull LinkGraph graph, @NotNull Vertex oldLoopVertex, @NotNull Vertex newLoopVertex, int oldLoopPreviousID) {
		int candidate = -1;
		for (int neighbour : oldLoopVertex.getNeighbourIDs()) {
			if (!graph.existsMapping(neighbour) && newLoopVertex.getNeighbourIDs().contains(neighbour)) {
				boolean newContainsPreviousOld = newLoopVertex.getNeighbourIDs().contains(oldLoopPreviousID);
				if (newContainsPreviousOld) {
					candidate = neighbour;
				} else {
					return neighbour;
				}
			}
		}
		return candidate;
	}

	private static @NotNull List<Integer> createInitialLoop(@NotNull SimulationGraph baseGraph, boolean oriented, @NotNull LinkGraph graph, int id, @NotNull List<LinkVertex> vertices) {
		@NotNull List<Integer> loop = Roundabout.createLoop(baseGraph);
		@NotNull List<LinkVertex> createdLoop = new ArrayList<>();  // FIXME
		for (int loopID : loop) {
			@NotNull LinkVertex linkVertex = new LinkVertex(id++, baseGraph.getVertex(loopID));
			createdLoop.add(linkVertex);
			vertices.add(linkVertex);
			graph.addVertexMapping(linkVertex);
			for (int neighbourID : baseGraph.getVertex(loopID).getNeighbourIDs()) {
				if (baseGraph.getVertex(neighbourID).getType().isExit()) {
					LinkVertex neighbour = vertices.get(graph.getLinkID(neighbourID));
					addNeighbour(linkVertex, neighbour, baseGraph, graph);
				}
			}
		}
		addNewLoopNeighbours(oriented, baseGraph, graph, vertices, loop.stream().map(baseGraph::getVertex).toList(), loop);

		return loop;
	}

	private static void addNewLoopNeighbours(boolean oriented, @NotNull Graph baseGraph, @NotNull LinkGraph graph, @NotNull List<LinkVertex> vertices, @NotNull List<? extends Vertex> newLoop, @NotNull List<Integer> newLoopIndexes) {
		for (@NotNull Vertex vertex : newLoop) {
			LinkVertex linkVertex = vertices.get(graph.getLinkID(vertex.getID()));
			for (int newLoopID : newLoopIndexes) {
				if (vertex.getNeighbourIDs().contains(newLoopID)) {
					Integer neighbourID = graph.getLinkID(newLoopID);
					LinkVertex neighbour = vertices.get(neighbourID);
					if (oriented) {
						if (isBeforeVertex(newLoopIndexes, vertex.getID(), newLoopID)) {
							addNeighbour(linkVertex, neighbour, baseGraph, graph);
						}
					} else {
						addNeighbour(linkVertex, neighbour, baseGraph, graph);
						addNeighbour(neighbour, linkVertex, baseGraph, graph);
					}
				}
			}
		}
	}

	@Deprecated
	private static void addLoopNeighbours(@NotNull List<? extends Vertex> loop, boolean oriented) {
		Vertex first = loop.get(0);
		Vertex last = first;
		for (int i = 1; i < loop.size(); i++) {
			Vertex vertex = loop.get(i);
			last.addNeighbourID(vertex.getID());
			if (!oriented) {
				vertex.addNeighbourID(last.getID());
			}
			last = vertex;
		}
		last.addNeighbourID(first.getID());
		if (!oriented) {
			first.addNeighbourID(last.getID());
		}
	}

	private static boolean isBeforeVertex(@NotNull List<Integer> loop, int firstVertexID, int secondVertexID) {
		return getLaneDistance(loop, firstVertexID, secondVertexID) <= loop.size() / 2;
	}

	private static int getLaneDistance(@NotNull List<Integer> loop, int id0, int id1) {
		int i0 = loop.indexOf(id0);
		int i1 = loop.indexOf(id1);
		int distance = i1 - i0;
		if (distance < 0) {
			return distance + loop.size();
		}
		return distance;
	}

}
