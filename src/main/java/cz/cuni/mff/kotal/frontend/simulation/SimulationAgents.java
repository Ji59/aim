package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.timer.Point;
import cz.cuni.mff.kotal.frontend.simulation.timer.SimulationTimer;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.*;


public class SimulationAgents extends Pane {
	private Simulation simulation;
	private final Map<Long, AgentPane> agents = new HashMap<>();
	private SimulationTimer timer;

	// TODO remove -  only for debug
	private final Set<Polygon> rectangles = new HashSet<>();

	public SimulationAgents(double height, Simulation simulation) {
		this.simulation = simulation;
		setPrefWidth(height);
		setPrefHeight(height);
	}

	public SimulationAgents(double height) {
		setPrefWidth(height);
		setPrefHeight(height);
	}

	public void addAgent(long startTime, Agent agent, double period) {
		long agentID = agent.getId();
		AgentPane agentPane = new AgentPane(agentID, startTime, agent, period, simulation.getIntersectionGraph().getVerticesWithIDs());

		synchronized (agents) {
			agents.put(agentID, agentPane);
		}

		Platform.runLater(() -> getChildren().add(agentPane));
	}

	public void addAgent(Long time, Agent agent) {
		addAgent(time, agent, IntersectionScene.getPeriod());
	}

	public void removeAgent(long agentID) {
		AgentPane agentPane;
		synchronized (agents) {
			agentPane = agents.remove(agentID);
			if (agentPane == null) {
				return;
			}
		}
		agentPane.setDisable(true);
		getChildren().remove(agentPane);
	}

	public void removeAgent(Map.Entry<Long, AgentPane> agentEntry) {
		synchronized (agents) {
			agents.remove(agentEntry.getKey());
		}
		AgentPane agentPane = agentEntry.getValue();
		agentPane.setDisable(true);
		getChildren().remove(agentPane);
	}

	public void setSimulation(Simulation simulation) {
		this.simulation = simulation;
	}

	public void pauseSimulation() {
		timer.stop();
		// TODO
//		synchronized (AGENTS) {
//			AGENTS.values().forEach(a -> a.pause());
//		}
	}

	public void resumeSimulation(Simulation simulation) {
		this.simulation = simulation;

		long startTime = System.nanoTime();
		synchronized (agents) {
			agents.values().forEach(agentPane -> agentPane.resume(simulation.getPeriod(), startTime));
		}
		timer = new SimulationTimer(agents, this);
		timer.start();
	}

	public void resetSimulation() {
		synchronized (agents) {
			timer.stop();
			agents.values().forEach(agentPane -> {
				getChildren().remove(agentPane);
			});
			agents.clear();
		}
	}

	public void resetRectangles() {
		// TODO remove - only for debug purposes
		getChildren().removeAll(rectangles);
		rectangles.clear();
	}

	// TODO remove - only for debug purposes
	public void addRectangle(List<Point> cornerPoints) {
		double[] points = new double[cornerPoints.size() * 2];
		for (int i = 0; i < cornerPoints.size(); i++) {
			points[2 * i] = cornerPoints.get(i).getX();
			points[2 * i + 1] = cornerPoints.get(i).getY();
		}
		addRectangle(points, 0);
	}

	// TODO remove - only for debug purposes
	public void addRectangle(double[] boundingBox) {
		double[] points = new double[boundingBox.length * 2];
		points[0] = boundingBox[0];
		points[1] = boundingBox[1];
		points[2] = boundingBox[2];
		points[3] = boundingBox[1];
		points[4] = boundingBox[2];
		points[5] = boundingBox[3];
		points[6] = boundingBox[0];
		points[7] = boundingBox[3];
		addRectangle(points, 255);
	}

	// TODO remove - only for debug purposes
	public void addRectangle(double[] points, int red) {
		Polygon rectangle = new Polygon(points);
		rectangle.setFill(Color.rgb(red, 0, 0, 0.3));
		rectangle.toBack();
		rectangles.add(rectangle);
		getChildren().add(rectangle);
	}

	public Simulation getSimulation() {
		return simulation;
	}
}