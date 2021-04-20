package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;


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

		double x = agent.getX() - agent.getL() / 2,
			y = agent.getY() - agent.getW() / 2;
		agentPane.setLayoutX(x);
		agentPane.setLayoutY(y);
		// TODO agentPane.setRotate();

		agents.put(agent.getId(), agentPane);

		getChildren().add(agentPane);
	}

	public void redraw(Map<Long, Agent> actualAgents) {
		agents.forEach((id, pane) -> {
			Agent a = actualAgents.get(id);
			if (a == null) {
				getChildren().remove(pane);
				agents.remove(id);
			} else {
				pane.setLayoutX(a.getX());
				pane.setLayoutY(a.getY());
			}
			actualAgents.remove(id);
		});

		actualAgents.forEach((id, agent) -> addAgent(agent));
	}

	public void setSimulation(Simulation simulation) {
		this.simulation = simulation;
		simulation.setGuiCallback(this::redraw);
	}
}
