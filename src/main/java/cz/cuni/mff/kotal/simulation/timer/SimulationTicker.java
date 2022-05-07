package cz.cuni.mff.kotal.simulation.timer;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.helpers.Collisions;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public interface SimulationTicker {
	int COLLISION_AGENTS_SHOWN_STEPS = 1;
	double MINIMUM_STEP_SIZE_PER_FRAME = 0.0625;  // 2 ^ -4
	double MAXIMUM_STEP_SIZE_PER_FRAME = 0.1875;  // 3 * 2 ^ -4

	MutableNumber<Long> frames = new MutableNumber<>(0L);
	MutableNumber<Double> maxStep = new MutableNumber<>(0.);
	List<Long> verticesUsage = new ArrayList<>();

	Lock verticesUsageLock = new ReentrantLock();

	void handleStep(double step);

	static void updateSimulation(double step) {
		updateSimulation(step, true);
	}

	static void updateSimulation(double step, boolean showAgents) {
		IntersectionMenu.setStep(step);
		IntersectionScene.getSimulation().loadAndUpdateAgents(step);
		IntersectionScene.getSimulationAgents().addArrivedAgents(step, showAgents);
		IntersectionScene.getSimulation().updateStatistics(step);
	}

	static double getVertexUsage(int id) {
		Long frameCount = frames.getValue();
		SimulationTicker.verticesUsageLock.lock();
		double usage = frameCount <= 0 ? 0 : (double) verticesUsage.get(id) / frameCount;
		SimulationTicker.verticesUsageLock.unlock();
		return usage;
	}

	static void updateVerticesUsage(double step, int updateVisualEach) {
		verticesUsageLock.lock();
		frames.setValue(frames.value + 1);
		SimulationAgents.getActiveAgents().values().stream()
			.filter(agentPane -> !agentPane.isDisable())
			.map(AgentPane::getAgent)
			.forEach(agent -> {
				int vertex = agent.getNearestPathVertexId(step - agent.getPlannedTime());
				verticesUsage.set(vertex, verticesUsage.get(vertex) + 1);
			});
		verticesUsageLock.unlock();
		if (frames.value % updateVisualEach == 0) {
			IntersectionModel.updateVertexNodesColors();
//			Runnable updateColors = () -> ;
//			/**
//			 new Thread(updateColors).start();
//			 /*/
//			updateColors.run();
//			/**/
//			verticesUsageLock.unlock();
		}
	}

	/**
	 * TODO
	 *
	 * @param graph
	 */
	static void resetValues(Graph graph) {
		verticesUsageLock.lock();
		verticesUsage.clear();
		for (int i = 0; i < graph.getVertices().length; i++) {
			verticesUsage.add(0L);
		}

		frames.setValue(0L);
		maxStep.setValue(0D);
		SimulationAnimationTimer.resetValues();
		verticesUsageLock.unlock();
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
		updateVerticesUsageAndTimeline(step, 1);
	}

	default void updateVerticesUsageAndTimeline(double step, int updateVerticesUsageSteps) {
		if (maxStep.value < step) {
			updateVerticesUsage(step, updateVerticesUsageSteps);
			IntersectionScene.getSimulationAgents().setVertexLabelText();
		}

		maxStep.setGreaterValue(step);
		IntersectionMenu.setTimelineMaximum(step, maxStep.value);
	}

	void start();

	void stop();

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
