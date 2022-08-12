package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO move class
public class LinkGraph extends SimulationGraph {
	private static SimulationGraph baseGraph;
	private final Map<Integer, Integer> vertexMapping = new HashMap<>();

	public LinkGraph(boolean oriented, SimulationGraph graph) {
		super(oriented, graph);
		baseGraph = graph;
	}

	public LinkVertex getLinkVertex(int id) {
		return (LinkVertex) vertices[id];
	}

	@Override
	public IntersectionMenuTab0.Parameters.GraphType getModel() {
		return baseGraph.getModel();
	}

	@Override
	public double getCellSize() {
		return baseGraph.getCellSize();
	}

	public Integer addVertexMapping(LinkVertex vertex) {
		return vertexMapping.put(vertex.getRealID(), vertex.getID());
	}

	public Integer getLinkID(int realID) {
		return vertexMapping.get(realID);
	}

	public boolean existsMapping(int realID) {
		return vertexMapping.containsKey(realID);
	}

	public List<Integer> getRealPath(List<Integer> linkPath) {
		return linkPath.stream().map(id -> getLinkVertex(id).getRealID()).toList();
	}

	public LinkVertex getLinkVertexByReal(int realID) {
		return (LinkVertex) vertices[vertexMapping.get(realID)];
	}
}
