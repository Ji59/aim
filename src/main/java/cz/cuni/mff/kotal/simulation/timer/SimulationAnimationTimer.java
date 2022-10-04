package cz.cuni.mff.kotal.simulation.timer;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.animation.AnimationTimer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Timer to be tick every frame to redraw agents and check for collisions.
 */
public class SimulationAnimationTimer extends AnimationTimer implements SimulationTicker {
	private static double lastStep = Double.MAX_VALUE;
	private static int overLimitCount = 0;

	private final Map<Long, AgentPane> agents;
	private final Simulation simulation;
	private final long startTime = System.nanoTime();


	/**
	 * Create new timer.
	 *
	 * @param agents     Collection of agents to get data from
	 * @param simulation GUI node starting this timer TODO
	 */
	public SimulationAnimationTimer(Map<Long, AgentPane> agents, Simulation simulation) {
		this.agents = agents;
		this.simulation = simulation;
		setLastStep(Double.MAX_VALUE);
	}

	/**
	 * TODO
	 *
	 * @param value
	 */
	private static void setLastStep(double value) {
		lastStep = value;
		overLimitCount = 0;
	}

	protected static void resetValues() {
		SimulationAnimationTimer.setLastStep(Double.MAX_VALUE);
	}

	/**
	 * Compute agents positions, update values, check for collisions.
	 *
	 * @param now System time of frame
	 */
	@Override
	public void handle(long now) {
//		SimulationAgents.resetRectangles(); //TODO
		double step = simulation.getStep(now - startTime);

		if (simulation.ended() && agents.isEmpty() && IntersectionScene.getSimulationAgents().emptyArrivingAgents()) {
			stopSimulation(step);
			return;
		}

		handleStep(step);
	}

	@Override
	public void stop() {
		long stopTime = System.nanoTime();
		super.stop();
		double lastHandledStep = lastStep < Double.MAX_VALUE ? lastStep : simulation.getStep(stopTime - startTime);
		simulation.setStartingStep(lastHandledStep);
	}

	@Override
	public void handleStep(double step) {
		handleStep(step, false);
	}

	@Override
	public void updateAgents(double step, Set<AgentPane> activeAgents) {
		synchronized (agents) {
			Iterator<Map.Entry<Long, AgentPane>> activeAgentsIterator = agents.entrySet().iterator();
			while (activeAgentsIterator.hasNext()) {
				Map.Entry<Long, AgentPane> a = activeAgentsIterator.next();
				AgentPane agentPane = a.getValue();
				if (agentPane.getCollisionStep() > step) {
					boolean finished = agentPane.handleTick(step);
					if (finished) {
						removeAgent(activeAgentsIterator, agentPane);
					} else {
						activeAgents.add(agentPane);
					}
				} else {
					if (agentPane.getCollisionStep() + SimulationTicker.COLLISION_AGENTS_SHOWN_STEPS <= step) {
						removeAgent(activeAgentsIterator, agentPane);
					} else {
						agentPane.collide();
					}
				}
			}
		}
	}

	@Override
	public void updateCollidedAgents(double step, AgentPane agentPane0, AgentPane agentPane1) {
		agentPane0.collide(step);
		agentPane1.collide(step);
	}

	private void handleStep(double step, boolean isMiddleStep) {
		updateSpeed(step, isMiddleStep);

		SimulationTicker.updateSimulation(step);
		if (simulation.getLoadedStep() < step) {
			simulation.stop();
			return;
		}

		Set<AgentPane> activeAgents = new HashSet<>(agents.size());
		updateAgents(step, activeAgents);

		handleCollisions(step, activeAgents);

		updateVerticesUsageAndTimeline(step);
	}

	private void updateSpeed(double step, boolean isMiddleStep) {
		final double stepSize = step - lastStep;

		if (isMiddleStep) {
			if (stepSize > SimulationTicker.MAXIMUM_STEP_SIZE_PER_FRAME) {
				handleStep(step - SimulationTicker.MAXIMUM_STEP_SIZE_PER_FRAME, true);
			}
			return;
		}

		/**
		 if (stepSize < MINIMUM_STEP_SIZE_PER_FRAME) {
		 if (++overLimitCount > 4 && IntersectionMenu.increaseSpeed()) {
		 setLastStep(Double.MAX_VALUE);
		 } else {
		 lastStep = step;
		 }
		 } else

		 /**/
		if (stepSize > SimulationTicker.MAXIMUM_STEP_SIZE_PER_FRAME) {
			handleStep(step - SimulationTicker.MAXIMUM_STEP_SIZE_PER_FRAME, true);
			if (stepSize > 1 && IntersectionMenu.decreaseSpeedToThreeFourths()) {
				setLastStep(Double.MAX_VALUE);
				stopSimulation(step);
				return;
			}

			if (--overLimitCount < -8 && IntersectionMenu.decreaseSpeed()) {
				setLastStep(Double.MAX_VALUE);
			} else {
				lastStep = step;
			}
		} else {
			setLastStep(step);
		}
	}

	private void stopSimulation(double step) {
		IntersectionMenu.pauseSimulation();
		forceUpdateSimulationStats(step, simulation);
	}

	private void removeAgent(Iterator<Map.Entry<Long, AgentPane>> activeAgentsIterator, AgentPane agentPane) {
		activeAgentsIterator.remove();
		IntersectionScene.getSimulationAgents().removeAgentPane(agentPane);
	}
}
