package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.MyGenerator;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cz.cuni.mff.kotal.MyGenerator.generateRandomInt;


public class SimulationAgents extends Pane {
	private Simulation simulation;
	private Map<Long, StackPane> agents = new HashMap<>();


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

	public void addAgent(Agent agent) {
		Rectangle rectangle = new Rectangle(agent.getL(), agent.getW());
		TextField text = new TextField(String.valueOf(agent.getId()));
		StackPane agentPane = new StackPane(rectangle, text);

		text.setBackground(Background.EMPTY);

		// TODO set color properly
		rectangle.setFill(Color.rgb(generateRandomInt(255), generateRandomInt(255), generateRandomInt(255)));

		// TODO set size properly
		double x = agent.getX() - agent.getL() / 2,
			y = agent.getY() - agent.getW() / 2;
		agentPane.setLayoutX(x);
		agentPane.setLayoutY(y);
		agentPane.toFront();

		// TODO set proper agent size
		double size = 50;
		agentPane.setPrefWidth(size);
		agentPane.setPrefHeight(size);

		// TODO agentPane.setRotate();

		agents.put(agent.getId(), agentPane);

		getChildren().add(agentPane);
	}

	public void redraw(Map<Long, Agent> actualAgents) {
		Set<Long> toRemoveAgentsIds = new HashSet<>();
		Platform.runLater(() -> {
			agents.forEach((id, pane) -> {
				Agent a = actualAgents.get(id);
				if (a == null) {
					getChildren().remove(pane);
					toRemoveAgentsIds.add(id);
//					agents.remove(id);
				} else {
					pane.setLayoutX(a.getX() - a.getW() / 2);
					pane.setLayoutY(a.getY() - a.getL() / 2);
				}
				actualAgents.remove(id);
			});
			toRemoveAgentsIds.forEach(id -> agents.remove(id));
			actualAgents.forEach((id, agent) -> addAgent(agent));
		});
	}

	public void setSimulation(Simulation simulation) {
		this.simulation = simulation;
		simulation.setGuiCallback(this::redraw);
	}
}
