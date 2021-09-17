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
import javafx.util.Pair;

import java.util.*;


public class SimulationAgents extends Pane {
	private static Simulation simulation;
	private final Map<Long, AgentPane> AGENTS = new HashMap<>();
	private SimulationTimer timer;

	// TODO remove -  only for debug
	private Set<Polygon> rectangles = new HashSet<>();

	public SimulationAgents(double height, Simulation simulation) {
		SimulationAgents.simulation = simulation;
		setPrefWidth(height);
		setPrefHeight(height);
	}

	public SimulationAgents(double height) {
		setPrefWidth(height);
		setPrefHeight(height);
	}

	public void addAgent(long startTime, Agent agent, double period) {
		long agentID = agent.getId();
		AgentPane agentPane = new AgentPane(agentID, startTime, agent, period, AGENTS.values());

		synchronized (AGENTS) {
			AGENTS.put(agentID, agentPane);
		}

		Platform.runLater(() -> getChildren().add(agentPane));
	}

	public void addAgent(Long time, Agent agent) {
		addAgent(time, agent, IntersectionScene.getPeriod());
	}

	public void removeAgent(long agentID) {
		AgentPane agentPane;
		synchronized (AGENTS) {
			agentPane = AGENTS.remove(agentID);
			if (agentPane == null) {
				return;
			}
		}
		agentPane.setDisable(true);
		getChildren().remove(agentPane);
	}

	public void removeAgent(Map.Entry<Long, AgentPane> agentEntry) {
		synchronized (AGENTS) {
			AGENTS.remove(agentEntry.getKey());
		}
		AgentPane agentPane = agentEntry.getValue();
		agentPane.setDisable(true);
		getChildren().remove(agentPane);
	}

	public void redraw(Pair<Long, Map<Long, Agent>> timeActualAgents) {
		// TODO remove
//		long time = timeActualAgents.getKey();
//		Map<Long, Agent> actualAgents = timeActualAgents.getValue();
//		Platform.runLater(() -> {
//			Iterator<Map.Entry<Long, AgentPane>> agentsIterator = AGENTS.entrySet().iterator();
//			while (agentsIterator.hasNext()) {
//				Map.Entry<Long, AgentPane> entry = agentsIterator.next();
//				long id = entry.getKey();
//				AgentPane pane = entry.getValue();
//				Agent a = actualAgents.get(id);
//				if (a == null) {
//					getChildren().remove(pane);
//					agentsIterator.remove();
//				} else {
//					pane.updateAgent(time);
//				}
//				actualAgents.remove(id);
//			}
//			actualAgents.forEach((id, agent) -> addAgent(time, agent));
//		});
	}

	public void setSimulation(Simulation simulation) {
		SimulationAgents.simulation = simulation;
	}

	public void pauseSimulation() {
		timer.stop();
		// TODO
//		synchronized (AGENTS) {
//			AGENTS.values().forEach(a -> a.pause());
//		}
	}

	public void resumeSimulation(double period) {
		long startTime = System.nanoTime();
		synchronized (AGENTS) {
			AGENTS.values().forEach(agentPane -> agentPane.resume(period, startTime));
		}
		timer = new SimulationTimer(AGENTS, this);
		timer.start();
	}

	public void resetSimulation() {
		synchronized (AGENTS) {
			timer.stop();
			AGENTS.values().forEach(agentPane -> {
				// TODO
//				agentPane.pause();
				getChildren().remove(agentPane);
			});
		}
		AGENTS.clear();
	}

// TODO remove
//	private class AgentTimer extends AnimationTimer {
//		private final long startTime;
//		private final double period,
//			relativeDistanceTraveled;
//		private final AgentPane agentPane;
//
//		public AgentTimer(long startTime, double period, double relativeDistanceTraveled, AgentPane agentPane) {
//			this.startTime = startTime;
//			this.period = period * 1_000_000; // convert to nanoseconds
//			this.relativeDistanceTraveled = relativeDistanceTraveled;
//			this.agentPane = agentPane;
//		}
//
//		public double getRelativeTimeTraveled(long time) {
//			return (time - startTime) / period;
//		}
//	}


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

	public static Simulation getSimulation() {
		return simulation;
	}
}