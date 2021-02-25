package cz.cuni.mff.kotal.frontend.simulation;


import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;


public class SimulationAgents extends Pane {
	private static final Map<Long, StackPane> agents = new HashMap<>();


	public SimulationAgents(double height) {
		setPrefWidth(height);
		setPrefHeight(height);
	}

	public void addAgent(Agent agent) {
		Rectangle rectangle = new Rectangle(agent.getL(), agent.getW());
		TextField text = new TextField(String.valueOf(agent.getId()));
		StackPane agentPane = new StackPane(rectangle, text);

		double x = agent.getX() - agent.getL() / 2,
			y = agent.getY() - agent.getW() / 2;
		agentPane.setLayoutX(x);
		agentPane.setLayoutY(y);

		agents.put(agent.getId(), agentPane);

		getChildren().add(agentPane);
	}
}
