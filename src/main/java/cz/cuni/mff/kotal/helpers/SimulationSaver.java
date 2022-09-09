package cz.cuni.mff.kotal.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.BasicAgent;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class SimulationSaver {

	public static void saveAgents(Simulation simulation, File file) throws IOException {
		FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8, false);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		gson.toJson(simulation.getAllCreatedAgents().stream().map(BasicAgent::new).toList(), writer);
		writer.close();
	}

	public static void saveStatistics(Simulation simulation, File directory) throws IOException {
		assert directory.isDirectory() && directory.list().length == 0;

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		saveStatisticsParameters(gson, simulation, directory);

		FileWriter agentsWriter = new FileWriter(Paths.get(directory.getAbsolutePath(), "agents.json").toFile());
		gson.toJson(simulation.getAllCreatedAgents().stream().map(PathAgent::new).toList(), agentsWriter);
		agentsWriter.close();
	}

	private static void saveStatisticsParameters(Gson gson, Simulation simulation, File directory) throws IOException {
		Parameters parameters = new Parameters(
			simulation.getAgents(), simulation.getStartingStep(), simulation.getDelay(), simulation.getRejections(), simulation.getCollisions(),
			Objects.requireNonNull(AlgorithmMenuTab2.Parameters.Algorithm.nameOf(simulation.getAlgorithm())).getName(),
			new TypedGraph(simulation.getIntersectionGraph()), simulation.getPlanningTime()
		);
		FileWriter statisticsWriter = new FileWriter(Paths.get(directory.getAbsolutePath(), "Parameters.json").toFile(), StandardCharsets.UTF_8, false);
		gson.toJson(parameters, statisticsWriter);
		statisticsWriter.close();
	}

	private static class TypedGraph extends Graph {
		private final String type;
		private final Vertex[] vertices;

		private TypedGraph(Graph graph) {
			super(true, graph);
			type = IntersectionMenuTab0.getModel().getText();
			vertices = graph.getVertices();
		}
	}

	private record Parameters(long agents, double steps, double delay, double rejections, double collisions, String algorithm, TypedGraph graph, List<Long> simulatingTime) {
	}

	private static class PathAgent extends BasicAgent {
		private final List<Integer> path;

		private PathAgent(Agent agent) {
			super(agent);
			this.path = agent.getPath();
		}
	}
}
