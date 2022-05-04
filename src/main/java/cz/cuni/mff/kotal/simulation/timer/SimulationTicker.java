package cz.cuni.mff.kotal.simulation.timer;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.helpers.Collisions;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface SimulationTicker {
	int COLLISION_AGENTS_SHOWN_STEPS = 1;
	double MINIMUM_STEP_SIZE_PER_FRAME = 0.0625;  // 2 ^ -4
	double MAXIMUM_STEP_SIZE_PER_FRAME = 0.25;  // 3 * 2 ^ -2
	MutableNumber<Long> frames = new MutableNumber<>(0L);
	MutableNumber<Double> maxStep = new MutableNumber<>(0.);
	List<Long> verticesUsage = new ArrayList<>();


	void handleStep(double step);
	static void updateSimulation(double step) {
		IntersectionMenu.setStep(step);
		IntersectionScene.getSimulation().loadAndUpdateAgents(step);
		IntersectionScene.getSimulationAgents().addArrivedAgents(step);
		IntersectionScene.getSimulation().updateStatistics(step);
	}

	static void updateVerticesUsage(double step) {
		frames.setValue(frames.value + 1);
		SimulationAgents.getActiveAgents().values().stream()
			.filter(agentPane -> !agentPane.isDisable())
			.map(AgentPane::getAgent)
			.forEach(agent -> {
				int vertex = agent.getNearestPathVertexId(step - agent.getPlannedTime());
				verticesUsage.set(vertex, verticesUsage.get(vertex) + 1);
			});
		IntersectionModel.updateVertexNodesColors(verticesUsage, frames.value);
	}

	/**
	 * TODO
	 *
	 * @param graph
	 */
	static void resetValues(Graph graph) {
		verticesUsage.clear();
		for (int i = 0; i < graph.getVertices().length; i++) {
			verticesUsage.add(0L);
		}

		frames.setValue(0L);
		maxStep.setValue(0D);
		SimulationAnimationTimer.resetValues();
		SimulationBackgroundTicker.resetValues();
	}

	void updateAgents(double step, Set<AgentPane> activeAgents);

	default void handleCollisions(double step, Set<AgentPane> activeAgents) {
		Set<Pair<AgentPane, AgentPane>> overlappingAgents = Collisions.getBoundingBoxesOverlaps(activeAgents);
		overlappingAgents = overlappingAgents.stream().filter(pair -> Collisions.inCollision(pair.getKey(), pair.getValue())).collect(Collectors.toSet());

		// Handle collisions
		overlappingAgents.forEach(agentPanePair -> {
			AgentPane agentPane0 = agentPanePair.getKey();
			AgentPane agentPane1 = agentPanePair.getValue();

			if (step < agentPane0.getCollisionStep()) {
				IntersectionScene.getSimulation().addCollision();
			}
			if (step < agentPane1.getCollisionStep()) {
				IntersectionScene.getSimulation().addCollision();
			}

			updateCollidedAgents(step, agentPane0, agentPane1);
		});
	}

	void updateCollidedAgents(double step, AgentPane agentPane0, AgentPane agentPane1);

	default void updateVerticesUsageAndTimeline(double step) {
		if (maxStep.value < step) {
			updateVerticesUsage(step);
			IntersectionScene.getSimulationAgents().setVertexLabelText();
		}

		maxStep.setGreaterValue(step);
		IntersectionMenu.setTimelineMaximum(step, maxStep.value);
	}

	class MutableNumber<T extends Number> {
		private T value;

		public MutableNumber(T initialValue) {
			value = initialValue;
		}

		public T setValue(T newValue) {
			value = newValue;
			return value;
		}

		public T setGreaterValue(T newValue) {
			if (newValue.doubleValue() > value.doubleValue()) {
				value = newValue;
			}
			return value;
		}

		public T getValue() {
			return value;
		}
	}
}
