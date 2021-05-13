package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
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

	public SimulationAgents(double height, Simulation simulation) {
		this.simulation = simulation;
		setPrefWidth(height);
		setPrefHeight(height);
		simulation.setGuiCallback(this::redraw);
	}

	public SimulationAgents(double height) {
		setPrefWidth(height);
		setPrefHeight(height);
	}

	public void addAgent(long time, Agent agent) {
		AgentPane agentPane = new AgentPane(time, agent);

		AGENTS.put(agent.getId(), agentPane);

		getChildren().add(agentPane);
	}

	public void redraw(Pair<Long, Map<Long, Agent>> timeActualAgents) {
		long time = timeActualAgents.getKey();
		Map<Long, Agent> actualAgents = timeActualAgents.getValue();
		Platform.runLater(() -> {
			Iterator<Map.Entry<Long, AgentPane>> agentsIterator = AGENTS.entrySet().iterator();
			while (agentsIterator.hasNext()) {
				Map.Entry<Long, AgentPane> entry = agentsIterator.next();
				long id = entry.getKey();
				AgentPane pane = entry.getValue();
				Agent a = actualAgents.get(id);
				if (a == null) {
					getChildren().remove(pane);
					agentsIterator.remove();
				} else {
					pane.updateAgent(time, a);
				}
				actualAgents.remove(id);
			}
			actualAgents.forEach((id, agent) -> addAgent(time, agent));
		});
	}

	public void setSimulation(Simulation simulation) {
		this.simulation = simulation;
		simulation.setGuiCallback(this::redraw);
	}

	public void resetSimulation() {
		AGENTS.values().forEach(agentPane -> getChildren().remove(agentPane));
		AGENTS.clear();
	}

	public class AgentPane extends StackPane {
		Rotate rotation = new Rotate();

		public AgentPane(long time, Agent agent) {

			Rectangle rectangle = new Rectangle(agent.getL(), agent.getW());
			TextField text = new TextField(String.valueOf(agent.getId()));
			text.setAlignment(Pos.CENTER);

			// TODO set rotation based on the arriving location
				rotation.setPivotX(agent.getL() / 2);
				rotation.setPivotY(agent.getW() / 2);
			rectangle.getTransforms().add(rotation);
			updateAgent(time, agent);

			getChildren().addAll(rectangle, text);

			text.setBackground(Background.EMPTY);

			// TODO set color properly
			rectangle.setFill(Color.rgb(generateRandomInt(255), generateRandomInt(255), generateRandomInt(255)));

			// TODO set proper agent size
			double size = 50;
			setPrefWidth(size);
			setPrefHeight(size);
		}

		private void updatePosition(Agent agent) {
			// TODO do position properly
			setLayoutX(agent.getX() - agent.getL() / 2);
			setLayoutY(agent.getY() - agent.getW() / 2);
		}

		public void updateRotation(long time, Agent agent) {
			Map<Long, Vertex> verices = simulation.getIntersectionGraph().getVerticesWithIDs();
			Pair<Long, Long> previousNext = agent.getPreviousNextVertexIDs(time);
			GraphicalVertex start = (GraphicalVertex) verices.get(previousNext.getKey()),
				end = (GraphicalVertex) verices.get(previousNext.getValue());

			double angel = MyNumberOperations.computeRotation(start.getX(), start.getY(), end.getX(), end.getY());
			if (angel > 0) {
//				rotation.setPivotX(0);
//				rotation.setPivotY(0);
				this.rotation.setAngle(angel);
			}
		}

		public void updateAgent(long time, Agent agent) {
			updatePosition(agent);
			updateRotation(time, agent);
		}

		public double getRotation() {
			return rotation.getAngle();
		}
	}
}