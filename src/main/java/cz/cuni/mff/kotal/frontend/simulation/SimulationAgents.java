package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.MyNumberOperations;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Pair;

import java.util.*;

import static cz.cuni.mff.kotal.MyGenerator.generateRandomInt;


public class SimulationAgents extends Pane {
	private Simulation simulation;
	private final Map<Long, AgentPane> AGENTS = new HashMap<>();
	private AgentTimer timer;

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
		AgentPane agentPane = new AgentPane(startTime, agent, period);

		synchronized (AGENTS) {
			AGENTS.put(agent.getId(), agentPane);
		}

		Platform.runLater(() -> getChildren().add(agentPane));
	}

	public void addAgent(Long time, Agent agent) {
		addAgent(time, agent, IntersectionScene.getPeriod());
	}

	private void removeAgent(long agentID) {
		AgentPane agentPane;
		synchronized (AGENTS) {
			agentPane = AGENTS.remove(agentID);
		}
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
		this.simulation = simulation;
	}

	public void stopSimulation() {
		synchronized (AGENTS) {
			AGENTS.values().forEach(AgentPane::stop);
		}
	}

	public void resumeSimulation(double period) {
		synchronized (AGENTS) {
			AGENTS.values().forEach(agentPane -> agentPane.resume(period));
		}
	}

	public void resetSimulation() {
		synchronized (AGENTS) {
			AGENTS.values().forEach(agentPane -> {
				agentPane.stop();
				getChildren().remove(agentPane);
			});
		}
		AGENTS.clear();
	}

	private class AgentTimer extends AnimationTimer {
		private final long startTime;
		private final double period,
			relativeDistanceTraveled;
		private final AgentPane agentPane;

		public AgentTimer(long startTime, double period, double relativeDistanceTraveled, AgentPane agentPane) {
			this.startTime = startTime;
			this.period = period * 1_000_000; // convert to nanoseconds
			this.relativeDistanceTraveled = relativeDistanceTraveled;
			this.agentPane = agentPane;
		}

		@Override
		public void handle(long now) {
			double time = relativeDistanceTraveled + getRelativeTimeTraveled(now);
			try {
				agentPane.getAgent().computeNextXY(time, simulation.getIntersectionGraph().getVerticesWithIDs());
			} catch (IndexOutOfBoundsException e) {
				this.stop();
				removeAgent(agentPane.agent.getId());
				return;
			}
			agentPane.updateAgent(time);
		}

		public double getRelativeTimeTraveled(long time) {
			return (time - startTime) / period;
		}
	}


	public class AgentPane extends StackPane {
		private final Rotate rotation = new Rotate();
		private final Agent agent;
		private AgentTimer timer;
		private double distanceTraveled;

		public AgentPane(long startTime, Agent agent, double period) {
			this.agent = agent;
			this.distanceTraveled = 0;

			Rectangle rectangle = new Rectangle(agent.getL(), agent.getW());
			TextField text = new TextField(String.valueOf(agent.getId()));
			text.setAlignment(Pos.CENTER);

			// TODO set rotation based on the arriving location
			rotation.setPivotX(agent.getL() / 2);
			rotation.setPivotY(agent.getW() / 2);
			rectangle.getTransforms().add(rotation);

			getChildren().addAll(rectangle, text);

			text.setBackground(Background.EMPTY);

			// TODO set color properly
			rectangle.setFill(Color.rgb(generateRandomInt(255), generateRandomInt(255), generateRandomInt(255)));

			// TODO set proper agent size
			double size = 50;
			setPrefWidth(size);
			setPrefHeight(size);

			timer = new AgentTimer(startTime, period, distanceTraveled, this);
			timer.start();
		}

		private void updatePosition() {
			// TODO do position properly
			setLayoutX(agent.getX() - agent.getL() / 2);
			setLayoutY(agent.getY() - agent.getW() / 2);
		}

		public void updateRotation(double time) {
			Map<Long, Vertex> vertices = simulation.getIntersectionGraph().getVerticesWithIDs();
			Pair<Long, Long> previousNext = agent.getPreviousNextVertexIDs(time);
			GraphicalVertex start = (GraphicalVertex) vertices.get(previousNext.getKey()),
				end = (GraphicalVertex) vertices.get(previousNext.getValue());

			double angel = MyNumberOperations.computeRotation(start.getX(), start.getY(), end.getX(), end.getY());
			if (angel > 0) {
				this.rotation.setAngle(angel);
			}
		}

		public void updateAgent(double time) {
			updatePosition();
			updateRotation(time);
		}

		public void stop() {
			if (timer != null) {
				long now = System.nanoTime();
				timer.stop();
				distanceTraveled = timer.getRelativeTimeTraveled(now);
				timer = null;
			}
		}

		public void resume(double period) {
			assert timer == null;
			long now = System.nanoTime();
			timer = new AgentTimer(now, period, distanceTraveled, this);
			timer.start();
		}

		public double getRotation() {
			return rotation.getAngle();
		}

		public Agent getAgent() {
			return agent;
		}
	}
}